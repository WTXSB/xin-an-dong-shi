<template>
	<span class="nav-drapery" aria-hidden="true">
		<i class="drapery-side drapery-left"><span v-for="fold in 7" :key="fold" class="fabric-fold" :style="{ '--fold': fold - 1 }"></span></i>
		<i class="drapery-side drapery-right"><span v-for="fold in 7" :key="fold" class="fabric-fold" :style="{ '--fold': fold - 1 }"></span></i>
		<i class="drapery-valance"></i>
		<i class="drapery-rod"></i>
		<i class="drapery-tie drapery-tie--left"><b></b><b></b><span></span></i>
		<i class="drapery-tie drapery-tie--right"><b></b><b></b><span></span></i>
	</span>
</template>

<style scoped lang="scss">
.nav-drapery {
 position: absolute;
 inset: 0;
 z-index: 5;
 overflow: hidden;
 border-radius: 15px;
 pointer-events: none;
 perspective: 420px;
 perspective-origin: 50% 40%;
 transform-style: preserve-3d;
 filter: drop-shadow(0 2px 3px rgba(106, 84, 37, .19));
}
.drapery-side {
 position: absolute;
 top: 5px;
 bottom: 3px;
 width: 52%;
 background: linear-gradient(90deg, #dfc269, #fff0bb 45%, #ead384 75%, #d2b85f);
 transform-style: preserve-3d;
 box-shadow: inset 0 2px 5px rgba(111, 88, 32, .24), 2px 2px 5px rgba(108, 84, 31, .2);
 border-bottom: 2px solid #d8a84c;
 animation: draw-left 2.4s cubic-bezier(.3, .02, .18, 1) both, gather-left .85s 2.4s ease-in-out forwards;
 &::after {
  content: '';
  position: absolute;
  left: 7%;
  top: 54%;
  width: 11%;
  height: 3px;
  border-radius: 50%;
  background: #b78734;
  box-shadow: 1px 5px 0 -1px #dab764, 1px 8px 0 -1px #b78734;
  opacity: 0;
  animation: reveal-tie .6s 2.65s forwards;
 }
}
.fabric-fold {
 position: absolute;
 top: 0;
 bottom: 0;
 left: calc(var(--fold) * 14.285714%);
 width: 15%;
 border-radius: 45% 45% 26% 30% / 8% 8% 5% 5%;
 background: linear-gradient(90deg, rgba(114, 93, 36, .28), #e9ce7e 17%, #fff2c4 42%, #f1dc98 63%, #c8aa58 91%, #bba051);
 box-shadow: inset 1px 0 2px rgba(255, 255, 235, .6), 2px 0 3px rgba(103, 78, 25, .14);
 transform-origin: left center;
 animation: fabric-depth 3.25s ease-in-out both;
}
.fabric-fold:nth-child(even) { filter: brightness(.94); animation-name: fabric-depth-reverse; }
.drapery-left { left: 0; }
.drapery-right { right: 0; transform: scaleX(-1); }
.drapery-valance {
 position: absolute;
 inset: 4px 2px auto;
 height: calc(100% - 6px);
 background:
  radial-gradient(ellipse at 25% 14%, #fff5ce 2%, transparent 25%),
  radial-gradient(ellipse at 73% 15%, #fff3c4 2%, transparent 27%),
  repeating-linear-gradient(100deg, rgba(111, 92, 39, .19) 0, transparent 3px, rgba(255,255,235,.6) 8px, transparent 17px),
  linear-gradient(#fff0b2, #ebd28c 60%, #c5a955);
 transform: translateZ(7px) rotateX(-8deg);
 transform-origin: top center;
 box-shadow: inset 0 2px 3px rgba(255, 255, 240, .8), 0 3px 6px rgba(119, 96, 37, .22);
 animation: raise-valance 2.4s cubic-bezier(.3, .02, .18, 1) both;
}
.drapery-rod {
 position: absolute;
 top: 3px;
 left: 4px;
 right: 4px;
 height: 3px;
 background: linear-gradient(#d5aa57, #f8df9c, #c19742);
 border-radius: 4px;
}
.drapery-tie {
 position: absolute;
 top: 53%;
 width: 21px;
 height: 22px;
 opacity: 0;
 filter: drop-shadow(1px 2px 1px rgba(105, 80, 24, .3));
 transform-origin: 50% 40%;
 animation: tie-curtain .8s 2.45s ease-out forwards;
 b { position: absolute; top: 2px; width: 10px; height: 7px; border: 1.2px solid #b18e41; background: #f7e0a4; border-radius: 70% 40% 65% 40%; }
 b:first-child { left: 0; transform: rotate(25deg); }
 b:nth-child(2) { right: 0; transform: rotate(-25deg); }
 span { position: absolute; left: 8px; top: 6px; width: 5px; height: 5px; background: #cba453; border-radius: 3px; }
 &::before, &::after { content: ''; position: absolute; width: 3px; height: 10px; top: 9px; border: 1px solid #bd9850; border-radius: 3px; background: #f4dfaa; }
 &::before { left: 6px; transform: rotate(18deg); }
 &::after { right: 5px; transform: rotate(-19deg); }
}
.drapery-tie--left { left: 0; }
.drapery-tie--right { right: 0; }
@keyframes draw-left {
 0%, 12% { clip-path: polygon(0 0, 100% 0, 100% 25%, 100% 54%, 100% 74%, 100% 100%, 0 100%); }
 62% { clip-path: polygon(0 0, 64% 0, 55% 25%, 32% 54%, 29% 74%, 40% 100%, 0 100%); }
 100% { clip-path: polygon(0 0, 32% 0, 25% 25%, 14% 54%, 16% 74%, 24% 100%, 0 100%); }
}
@keyframes raise-valance {
 0%, 8% { clip-path: polygon(0 0,100% 0,100% 100%,85% 100%,70% 100%,50% 100%,30% 100%,15% 100%,0 100%); }
 65% { clip-path: polygon(0 0,100% 0,100% 30%,85% 40%,70% 28%,50% 40%,30% 28%,15% 40%,0 30%); }
 100% { clip-path: polygon(0 0,100% 0,100% 14%,85% 24%,70% 14%,50% 24%,30% 14%,15% 24%,0 14%); }
}
@keyframes reveal-tie { to { opacity: 1; } }
@keyframes gather-left {
 from { clip-path: polygon(0 0, 32% 0, 25% 25%, 14% 54%, 16% 74%, 24% 100%, 0 100%); }
 to { clip-path: polygon(0 0, 32% 0, 24% 25%, 6% 54%, 14% 74%, 24% 100%, 0 100%); }
}
@keyframes tie-curtain {
 0% { opacity: 0; transform: translateY(-5px) scale(.55) rotate(-25deg); }
 60% { opacity: 1; transform: translateY(1px) scale(1.12) rotate(8deg); }
 100% { opacity: 1; transform: translateY(0) scale(1) rotate(0); }
}
@keyframes fabric-depth {
 0%, 12% { transform: translateZ(1px) rotateY(-4deg) rotateX(0); }
 60% { transform: translateZ(5px) rotateY(-24deg) rotateX(3deg); }
 82% { transform: translateZ(3px) rotateY(-10deg) rotateX(-2deg); }
 100% { transform: translateZ(4px) rotateY(-16deg) rotateX(0); }
}
@keyframes fabric-depth-reverse {
 0%, 12% { transform: translateZ(0) rotateY(5deg); }
 60% { transform: translateZ(2px) rotateY(23deg) rotateX(-3deg); }
 82% { transform: translateZ(5px) rotateY(8deg) rotateX(2deg); }
 100% { transform: translateZ(2px) rotateY(15deg); }
}
@media (prefers-reduced-motion: reduce) {
 .drapery-side, .drapery-valance, .drapery-side::after, .drapery-tie, .fabric-fold { animation-duration: .001s; animation-delay: 0s; }
}
</style>
