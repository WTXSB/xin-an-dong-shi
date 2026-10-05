import { createApp } from 'vue';
import pinia from '/@/stores/index';
import App from './App.vue';
import router from './router';
import { i18n } from '/@/i18n/index';
import other from '/@/utils/other';
import 'animate.css/animate.min.css';

import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import '/@/theme/index.scss';
import VueGridLayout from 'vue-grid-layout';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import HealingIllustratedFrame from './components/healingArt/HealingIllustratedFrame.vue';
import HealingDecorationStrip from './components/healingArt/HealingDecorationStrip.vue';
import AnimalEmotionGallery from './components/healingArt/AnimalEmotionGallery.vue';
import '/@/theme/fonts/iconfont.css'

const app = createApp(App);
app.component('HealingIllustratedFrame', HealingIllustratedFrame);
app.component('HealingDecorationStrip', HealingDecorationStrip);
app.component('AnimalEmotionGallery', AnimalEmotionGallery);
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
	app.component(key, component);
}

other.elSvg(app);

app.use(pinia).use(router).use(ElementPlus, { i18n: i18n.global.t }).use(i18n).use(VueGridLayout).mount('#app');
