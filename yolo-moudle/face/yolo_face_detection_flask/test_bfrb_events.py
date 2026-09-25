import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from bfrb_events import BfrbEventAggregator


def cue(confidence=0.8, behavior="nail_biting", cue_type="咬指甲/进食类"):
    return {
        "behavior": behavior,
        "cueType": cue_type,
        "confidence": confidence,
        "bbox": [10, 20, 30, 40],
        "evidenceType": "behavior-model",
    }


class BfrbEventAggregatorTest(unittest.TestCase):
    def test_repeated_frames_are_one_event(self):
        aggregator = BfrbEventAggregator(fps=10, min_hits=3, max_gap_seconds=0.5)
        for frame in range(10):
            aggregator.update(frame, [cue(0.7 + frame / 100)])

        result = aggregator.finish(9)

        self.assertEqual(result["eventCount"], 1)
        self.assertEqual(result["events"][0]["sampleCount"], 10)
        self.assertEqual(result["byBehavior"][0]["count"], 1)
        self.assertEqual(result["events"][0]["keyFrameSeconds"], 0.9)

    def test_short_noise_is_not_an_event(self):
        aggregator = BfrbEventAggregator(fps=10, min_hits=3, max_gap_seconds=0.5)
        aggregator.update(0, [cue()])
        aggregator.update(1, [cue()])
        aggregator.update(10, [])

        result = aggregator.finish(10)

        self.assertEqual(result["eventCount"], 0)

    def test_gap_creates_two_events(self):
        aggregator = BfrbEventAggregator(fps=10, min_hits=3, max_gap_seconds=0.5)
        for frame in (0, 1, 2):
            aggregator.update(frame, [cue()])
        aggregator.update(9, [])
        for frame in (10, 11, 12):
            aggregator.update(frame, [cue(0.9)])

        result = aggregator.finish(12)

        self.assertEqual(result["eventCount"], 2)
        self.assertEqual(result["byBehavior"][0]["count"], 2)

    def test_different_behaviors_are_counted_separately(self):
        aggregator = BfrbEventAggregator(fps=20, min_hits=2, max_gap_seconds=0.25)
        for frame in (0, 1):
            aggregator.update(frame, [cue(), cue(0.75, "hair_pulling", "拔头发/抓挠头皮类")])

        result = aggregator.finish(1)

        self.assertEqual(result["eventCount"], 2)
        self.assertEqual({item["cueType"] for item in result["byBehavior"]}, {"咬指甲/进食类", "拔头发/抓挠头皮类"})


if __name__ == "__main__":
    unittest.main()
