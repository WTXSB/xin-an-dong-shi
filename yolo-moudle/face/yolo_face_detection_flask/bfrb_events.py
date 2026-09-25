"""Turn frame-level BFRB cues into human-readable behavior events.

The detector answers "what is visible in this frame".  This module answers
"how many behavior episodes happened in this video/camera session" so that a
single action lasting several frames is not counted several times.
"""

from collections import defaultdict
from copy import deepcopy


class BfrbEventAggregator:
    """Merge repeated frame detections into temporal BFRB events."""

    def __init__(self, fps, min_hits=3, max_gap_seconds=0.5):
        self.fps = max(float(fps or 0), 1.0)
        self.min_hits = max(int(min_hits), 1)
        self.max_gap_frames = max(int(round(max_gap_seconds * self.fps)), 1)
        self._states = {}
        self._events = []
        self._last_frame = 0

    @staticmethod
    def _best_cues_by_behavior(cues):
        best = {}
        for cue in cues or []:
            behavior = str(cue.get("behavior") or cue.get("cueType") or "unknown")
            confidence = float(cue.get("confidence") or 0)
            if behavior not in best or confidence > float(best[behavior].get("confidence") or 0):
                best[behavior] = cue
        return best

    def update(self, frame_index, cues):
        """Consume normalized cues for one analyzed frame."""
        frame_index = max(int(frame_index), 0)
        self._last_frame = max(self._last_frame, frame_index)
        current = self._best_cues_by_behavior(cues)

        for behavior, state in list(self._states.items()):
            if behavior not in current and frame_index - state["last_seen_frame"] > self.max_gap_frames:
                self._close_state(behavior)

        for behavior, cue in current.items():
            state = self._states.get(behavior)
            if state is None:
                state = self._new_state(frame_index, cue)
                self._states[behavior] = state
            elif frame_index - state["last_seen_frame"] > self.max_gap_frames:
                self._close_state(behavior)
                state = self._new_state(frame_index, cue)
                self._states[behavior] = state
            else:
                self._append_cue(state, frame_index, cue)

            if not state["confirmed"] and state["hit_count"] >= self.min_hits:
                state["confirmed"] = True

    def _new_state(self, frame_index, cue):
        confidence = float(cue.get("confidence") or 0)
        return {
            "behavior": str(cue.get("behavior") or cue.get("cueType") or "unknown"),
            "cue_type": str(cue.get("cueType") or cue.get("behavior") or "未知线索"),
            "evidence_type": str(cue.get("evidenceType") or "model"),
            "start_frame": frame_index,
            "last_seen_frame": frame_index,
            "hit_count": 1,
            "confirmed": self.min_hits == 1,
            "confidences": [confidence],
            "max_confidence": confidence,
            "key_frame": frame_index,
            "key_bbox": deepcopy(cue.get("bbox") or []),
            "geometry": deepcopy(cue.get("geometry") or {}),
        }

    @staticmethod
    def _append_cue(state, frame_index, cue):
        confidence = float(cue.get("confidence") or 0)
        state["last_seen_frame"] = frame_index
        state["hit_count"] += 1
        state["confidences"].append(confidence)
        if confidence >= state["max_confidence"]:
            state["max_confidence"] = confidence
            state["key_frame"] = frame_index
            state["key_bbox"] = deepcopy(cue.get("bbox") or [])
            state["geometry"] = deepcopy(cue.get("geometry") or {})

    def _close_state(self, behavior):
        state = self._states.pop(behavior, None)
        if state and state["confirmed"]:
            self._events.append(self._serialize_event(state, len(self._events) + 1))

    def _serialize_event(self, state, event_index):
        start_seconds = state["start_frame"] / self.fps
        end_seconds = state["last_seen_frame"] / self.fps
        # One analyzed frame still represents a non-zero observation interval.
        duration_seconds = max((state["last_seen_frame"] - state["start_frame"] + 1) / self.fps, 1 / self.fps)
        confidences = state["confidences"]
        return {
            "id": f"bfrb-{event_index}",
            "behavior": state["behavior"],
            "cueType": state["cue_type"],
            "evidenceType": state["evidence_type"],
            "startSeconds": round(start_seconds, 2),
            "endSeconds": round(end_seconds, 2),
            "durationSeconds": round(duration_seconds, 2),
            "sampleCount": state["hit_count"],
            "averageConfidence": round(sum(confidences) / len(confidences), 3),
            "maxConfidence": round(state["max_confidence"], 3),
            "keyFrameSeconds": round(state["key_frame"] / self.fps, 2),
            "keyBbox": state["key_bbox"],
            "geometry": state["geometry"],
        }

    def finish(self, last_frame_index=None):
        if last_frame_index is not None:
            self._last_frame = max(self._last_frame, int(last_frame_index))
        for behavior in list(self._states):
            self._close_state(behavior)
        return self.summary()

    def summary(self):
        totals = defaultdict(lambda: {"count": 0, "totalDurationSeconds": 0.0, "maxConfidence": 0.0})
        for event in self._events:
            item = totals[event["cueType"]]
            item["count"] += 1
            item["totalDurationSeconds"] += event["durationSeconds"]
            item["maxConfidence"] = max(item["maxConfidence"], event["maxConfidence"])

        by_behavior = []
        for cue_type, values in totals.items():
            by_behavior.append({
                "cueType": cue_type,
                "count": values["count"],
                "totalDurationSeconds": round(values["totalDurationSeconds"], 2),
                "maxConfidence": round(values["maxConfidence"], 3),
            })
        by_behavior.sort(key=lambda item: (-item["count"], item["cueType"]))

        return {
            "eventCount": len(self._events),
            "totalDurationSeconds": round(sum(event["durationSeconds"] for event in self._events), 2),
            "events": deepcopy(self._events),
            "byBehavior": by_behavior,
            "rules": {
                "minHits": self.min_hits,
                "maxGapSeconds": round(self.max_gap_frames / self.fps, 2),
                "note": "连续命中达到阈值后才形成事件；短暂漏检会合并为同一事件，不按视频帧重复计数。",
            },
        }
