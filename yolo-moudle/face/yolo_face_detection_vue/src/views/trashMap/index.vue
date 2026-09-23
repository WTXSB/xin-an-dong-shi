<template>
	<div class="spa-map-page">
		<header class="map-hero">
			<div>
				<p>心灵 SPA 地图导引</p>
				<h1>找一处此刻能让你慢下来的地方</h1>
				<span>
					位置只在你授权后读取，用来在地图上查找附近的真实资源；页面不会保存你的位置。
					路线、营业状态与服务内容请以地图平台和机构公开信息为准。
				</span>
			</div>
			<div class="hero-note">地图导引仅供参考</div>
		</header>

		<div class="content">
			<section class="map-section">
				<div v-show="mapAvailable" ref="mapContainer" class="amap-container"></div>

				<div v-if="!mapAvailable" class="map-fallback">
					<div class="fallback-card">
						<h2>地图服务还在准备中</h2>
						<p>
							这盏灯很快会亮起来。页面需要一枚高德地图的 Key 才能为你展开附近的地图，
							现在还没有配置好，所以先给你留了一张安静的小卡片。
						</p>
						<div class="fallback-steps">
							<p><strong>配置方式（只需一次）：</strong></p>
							<ol>
								<li>打开高德开放平台控制台，创建应用并添加「Web端(JS API)」类型的 Key；</li>
								<li>
									在前端项目根目录的 <code>.env.local</code> 中填入
									<code>VITE_AMAP_KEY</code> 和 <code>VITE_AMAP_SECURITY_CODE</code>；
								</li>
								<li>重新启动开发服务后回到本页，地图就会出现在这里。</li>
							</ol>
						</div>
						<p class="fallback-soft">配置完成前，你仍然可以阅读右侧的陪伴说明，它们一直有效。</p>
					</div>
				</div>

				<div v-if="mapAvailable" class="map-floating">
					<div class="position-card">
						<strong>{{ locationTitle }}</strong>
						<span>{{ locationHint }}</span>
					</div>
					<div class="legend">
						<div class="legend-item">
							<div class="legend-icon icon-user"></div>
							<span>你的大概位置</span>
						</div>
						<div class="legend-item">
							<div class="legend-icon icon-radius"></div>
							<span>定位误差参考范围</span>
						</div>
						<div v-for="cat in categories" :key="cat.key" class="legend-item">
							<div class="legend-icon" :style="{ background: cat.color }"></div>
							<span>{{ cat.title }}</span>
						</div>
					</div>
				</div>
			</section>

			<aside class="controls">
				<section class="control-section">
					<h2>定位与隐私</h2>
					<p class="soft-text">
						页面会先通过 IP 温柔地猜一猜你大概在哪个城市（只能到城市/城区级，不够精确）。
						点击按钮后浏览器会询问是否允许读取更精确的位置，用来查找离你更近的资源。
						坐标只在本页使用，不会在前端保存。
					</p>
					<button class="btn" :disabled="!mapAvailable || locating" @click="locatePrecise">
						{{ locating ? '正在温柔定位...' : '获取更精确位置' }}
					</button>
					<button class="btn btn-light" :disabled="!mapAvailable || searching" @click="refreshNearby">
						{{ searching ? '正在寻找附近的资源...' : '重新查找附近资源' }}
					</button>
					<div class="accuracy-box">
						<p><strong>定位状态：</strong>{{ locationStatus }}</p>
						<p v-if="userLocation"><strong>定位方式：</strong>{{ userLocation.source }}</p>
						<p v-if="userLocation"><strong>误差范围：</strong>约 {{ formatDistance(userLocation.accuracy) }}</p>
						<p v-if="userLocation"><strong>更新时间：</strong>{{ lastUpdated }}</p>
					</div>
				</section>

				<section class="control-section">
					<h2>附近真实资源</h2>
					<p class="soft-text">
						以当前定位点为中心、约 5 公里内的真实机构，来自高德地图公开数据。
						点击条目可以在地图上看到它的位置与详情。
					</p>
					<p v-if="mapAvailable && searching" class="soft-text">正在为你慢慢寻找，请稍等片刻...</p>
					<div v-else-if="mapAvailable && poiList.length" class="poi-list">
						<article
							v-for="poi in poiList"
							:key="poi.id"
							class="poi-card"
							@click="focusPoi(poi)"
						>
							<div class="resource-head">
								<span :style="{ background: poi.category.color }"></span>
								<h3>{{ poi.name }}</h3>
							</div>
							<p class="poi-cat">{{ poi.category.title }} · 约 {{ formatDistance(poi.distance) }}</p>
							<p class="poi-addr">{{ poi.address || '地址暂未公开' }}</p>
							<p class="poi-tel">{{ poi.tel || '暂无公开电话' }}</p>
						</article>
					</div>
					<div v-else-if="mapAvailable && searchedOnce" class="empty-box">
						<p>
							这个范围内暂时没有查到相关资源。不是你不被接住，只是网撒得还不够远——
							可以试试「获取更精确位置」后重新查找，或直接把地图缩小一点，去更大的范围里看看。
						</p>
					</div>
					<p v-else-if="!mapAvailable" class="soft-text">配置好地图 Key 后，这里会列出你附近的真实资源。</p>
				</section>

				<section class="control-section">
					<h2>更稳妥的使用方式</h2>
					<div class="warm-contact">
						<p>先查看距离、开放时间、用户评价与联系方式，再决定是否前往。</p>
						<p>如果你感到很累，可以先选择公园、书店、安静茶饮店这类低压力空间，让身体先缓一缓。</p>
						<p>青少年陪伴支持可关注 <strong>12355</strong> 当地公开服务入口，具体可用时间以官方信息为准。</p>
					</div>
				</section>

				<section class="control-section final-note">
					<h2>温柔提醒</h2>
					<p>
						本页提供的是生活支持与出行参考。地图数据、路线时间、营业状态和服务内容可能变化，请以地图平台、机构官方页面或电话确认为准。
					</p>
				</section>
			</aside>
		</div>

		<footer>
			<p>心安动识 | 愿你在需要的时候，知道哪里有一盏灯</p>
		</footer>
	</div>
</template>

<script>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import AMapLoader from '@amap/amap-jsapi-loader';

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY || '';
const AMAP_SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE || '';

const AMAP_PLUGINS = ['AMap.Scale', 'AMap.ToolBar', 'AMap.Geolocation', 'AMap.PlaceSearch'];

// 搜索分类：关键词支持高德「|」分隔的或条件
const CATEGORIES = [
	{
		key: 'mind',
		title: '心理咨询与心理门诊',
		keywords: '心理咨询|心理门诊|精神科',
		color: '#d89b72',
	},
	{
		key: 'breath',
		title: '冥想与瑜伽放松空间',
		keywords: '冥想|瑜伽',
		color: '#8fbf9f',
	},
	{
		key: 'park',
		title: '公园与城市绿道',
		keywords: '公园|绿道',
		color: '#e7c86e',
	},
	{
		key: 'book',
		title: '书店与安静茶饮',
		keywords: '书店|茶饮',
		color: '#d98d93',
	},
];

const SEARCH_RADIUS = 5000;
const IP_LOCATION_ACCURACY = 3000;

export default {
	setup() {
		const mapContainer = ref(null);
		const mapAvailable = ref(false);
		const locating = ref(false);
		const searching = ref(false);
		const searchedOnce = ref(false);
		const locationStatus = ref('正在准备地图...');
		const lastUpdated = ref('--');
		const userLocation = ref(null);
		const poiList = ref([]);
		const categories = CATEGORIES;

		let AMap = null;
		let map = null;
		let geolocation = null;
		let userMarker = null;
		let accuracyCircle = null;
		let infoWindow = null;
		let poiMarkers = [];
		let searchToken = 0;

		const locationTitle = computed(() => {
			if (!userLocation.value) return '正在轻轻定位你的城市';
			if (userLocation.value.source === 'IP 定位（城市级）') return '已定位到你所在的城市';
			if (userLocation.value.accuracy <= 100) return '定位已经比较清晰';
			if (userLocation.value.accuracy <= 1000) return '定位可用，仍有一些误差';
			return '定位范围偏大，可以再试一次';
		});

		const locationHint = computed(() => {
			if (!userLocation.value) return '通过 IP 定位到城市级，不需要你做任何授权。';
			if (userLocation.value.source === 'IP 定位（城市级）')
				return '当前是 IP 定位，只精确到城市/城区级；想要更近的结果，可点击右侧「获取更精确位置」。';
			return `当前位置误差约 ${formatDistance(userLocation.value.accuracy)}，附近资源以此为中心查找。`;
		});

		const escapeHtml = (text) =>
			String(text ?? '').replace(/[&<>"']/g, (ch) => ({
				'&': '&amp;',
				'<': '&lt;',
				'>': '&gt;',
				'"': '&quot;',
				"'": '&#39;',
			}[ch]));

		const formatDistance = (meters) => {
			const m = Number(meters);
			if (!Number.isFinite(m) || m <= 0) return '--';
			if (m < 1000) return `${Math.round(m)} 米`;
			return `${(m / 1000).toFixed(1)} 公里`;
		};

		const stampUpdated = () => {
			lastUpdated.value = new Date().toLocaleTimeString('zh-CN', {
				hour: '2-digit',
				minute: '2-digit',
				second: '2-digit',
			});
		};

		const drawUserLocation = (lng, lat, accuracy, source) => {
			userLocation.value = { lng, lat, accuracy, source };
			stampUpdated();

			if (userMarker) {
				map.remove(userMarker);
				userMarker = null;
			}
			if (accuracyCircle) {
				map.remove(accuracyCircle);
				accuracyCircle = null;
			}

			userMarker = new AMap.Marker({
				position: [lng, lat],
				content: '<div class="spa-user-marker"><span></span></div>',
				offset: new AMap.Pixel(-13, -13),
				zIndex: 120,
			});
			map.add(userMarker);

			accuracyCircle = new AMap.Circle({
				center: [lng, lat],
				radius: Math.max(accuracy, 100),
				strokeColor: '#c98f5c',
				strokeWeight: 1,
				strokeOpacity: 0.8,
				fillColor: '#f1cba7',
				fillOpacity: 0.18,
				zIndex: 100,
			});
			map.add(accuracyCircle);
		};

		const searchNearby = (center) => {
			if (!AMap || !map || !center) return;
			const token = ++searchToken;
			searching.value = true;
			searchedOnce.value = true;
			poiList.value = [];

			if (poiMarkers.length) {
				map.remove(poiMarkers);
				poiMarkers = [];
			}

			const placeSearch = new AMap.PlaceSearch({
				pageSize: 15,
				pageIndex: 1,
				extensions: 'base',
			});

			let finished = 0;
			const collected = [];

			CATEGORIES.forEach((cat) => {
				placeSearch.searchNearBy(cat.keywords, center, SEARCH_RADIUS, (status, result) => {
					if (token !== searchToken) return; // 已有更新的定位，忽略旧结果
					finished += 1;
					if (status === 'complete' && result?.poiList?.pois) {
						result.poiList.pois.forEach((poi) => {
							if (!poi.location) return;
							collected.push({
								id: `${cat.key}-${poi.id}`,
								rawId: poi.id,
								name: poi.name,
								address: poi.address || '',
								tel: poi.tel || '',
								distance: poi.distance || 0,
								lng: poi.location.lng,
								lat: poi.location.lat,
								category: cat,
							});
						});
					}
					if (finished === CATEGORIES.length) {
						searching.value = false;
						// 同一机构可能命中多个分类，按高德原始 id 去重
						const seen = new Set();
						poiList.value = collected
							.filter((poi) => {
								if (seen.has(poi.rawId)) return false;
								seen.add(poi.rawId);
								return true;
							})
							.sort((a, b) => a.distance - b.distance);
						renderPoiMarkers(poiList.value);
					}
				});
			});
		};

		const renderPoiMarkers = (list) => {
			list.forEach((poi) => {
				const marker = new AMap.Marker({
					position: [poi.lng, poi.lat],
					content: `<div class="spa-poi-marker" style="background:${poi.category.color}"></div>`,
					offset: new AMap.Pixel(-7, -7),
					zIndex: 110,
					title: poi.name,
				});
				marker.on('click', () => openPoiInfo(poi));
				map.add(marker);
				poiMarkers.push(marker);
			});
		};

		const copyText = async (text, okMsg, failMsg) => {
			try {
				await navigator.clipboard.writeText(text);
				locationStatus.value = okMsg;
			} catch {
				locationStatus.value = failMsg;
			}
		};

		const buildInfoContent = (poi) => {
			const wrap = document.createElement('div');
			wrap.className = 'spa-info';
			wrap.innerHTML = `
				<div class="spa-info-title">
					<span class="spa-info-dot" style="background:${poi.category.color}"></span>
					<strong>${escapeHtml(poi.name)}</strong>
				</div>
				<p class="spa-info-line">${escapeHtml(poi.category.title)} · 约 ${formatDistance(poi.distance)}</p>
				<p class="spa-info-line">地址：${escapeHtml(poi.address || '暂未公开')}</p>
				<p class="spa-info-line">电话：${escapeHtml(poi.tel || '暂无公开电话')}</p>
				<div class="spa-info-actions">
					<a href="https://ditu.amap.com/place/${encodeURIComponent(poi.rawId)}" target="_blank" rel="noopener noreferrer">在高德地图查看详情</a>
				</div>
			`;
			if (poi.tel) {
				const btn = document.createElement('button');
				btn.type = 'button';
				btn.className = 'spa-info-copy';
				btn.textContent = '复制电话';
				btn.addEventListener('click', () =>
					copyText(poi.tel, `已复制电话：${poi.tel}`, `可手动复制电话：${poi.tel}`)
				);
				wrap.querySelector('.spa-info-actions').appendChild(btn);
			}
			return wrap;
		};

		const openPoiInfo = (poi) => {
			if (!infoWindow) {
				infoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -12), closeWhenClickMap: true });
			}
			infoWindow.setContent(buildInfoContent(poi));
			infoWindow.open(map, [poi.lng, poi.lat]);
		};

		const focusPoi = (poi) => {
			if (!map) return;
			map.setZoomAndCenter(Math.max(map.getZoom(), 15), [poi.lng, poi.lat], false, 300);
			openPoiInfo(poi);
		};

		const runCityLocation = () => {
			locationStatus.value = '正在通过 IP 定位到你的城市（城市/城区级精度）';
			// 兜底：IP 定位服务偶尔不回调，避免状态卡在“正在定位”
			let settled = false;
			const fallbackTimer = setTimeout(() => {
				if (settled) return;
				settled = true;
				locationStatus.value = '暂时没猜到你的城市，地图停在一个默认位置，可以手动缩放浏览';
			}, 8000);
			// 高德 JS API 2.0 中 IP 定位走 Geolocation 插件的 getCityInfo
			//（旧的 CitySearch.getCityByIp 在 2.0 下经常静默无回调）
			geolocation.getCityInfo((status, result) => {
				if (settled) return;
				settled = true;
				clearTimeout(fallbackTimer);
				const position = result?.position;
				if (status !== 'complete' || !Array.isArray(position)) {
					locationStatus.value = '暂时没猜到你的城市，地图停在一个默认位置，可以手动缩放浏览';
					return;
				}
				const [lng, lat] = position.map(Number);

				drawUserLocation(lng, lat, IP_LOCATION_ACCURACY, 'IP 定位（城市级）');
				locationStatus.value = `已通过 IP 定位到「${result.city || '你所在的城市'}」，精度为城市/城区级`;
				map.setZoomAndCenter(12, [lng, lat], false, 300);
				searchNearby([lng, lat]);
			});
		};

		const locatePrecise = () => {
			if (!geolocation || locating.value) return;
			locating.value = true;
			locationStatus.value = '正在请求浏览器定位授权，想分享多少都由你决定';
			geolocation.getCurrentPosition((status, result) => {
				locating.value = false;
				if (status !== 'complete' || !result?.position) {
					locationStatus.value = '这次没有拿到更精确的位置，没关系，城市级结果也够用';
					return;
				}
				const { lng, lat } = result.position;
				const accuracy = result.accuracy || 200;
				drawUserLocation(lng, lat, accuracy, '浏览器授权定位');
				locationStatus.value = accuracy <= 100 ? '已获得较清晰的位置' : '已获得更精确的位置，但仍有一些误差';
				map.setZoomAndCenter(accuracy <= 100 ? 16 : 14, [lng, lat], false, 300);
				searchNearby([lng, lat]);
			});
		};

		const refreshNearby = () => {
			if (!userLocation.value) {
				locationStatus.value = '还没有可用的定位点，稍等 IP 定位完成后再试试';
				return;
			}
			searchNearby([userLocation.value.lng, userLocation.value.lat]);
		};

		const initMap = async () => {
			if (!AMAP_KEY || !AMAP_SECURITY_CODE) {
				mapAvailable.value = false;
				locationStatus.value = '地图 Key 尚未配置';
				return;
			}
			try {
				window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE };
				AMap = await AMapLoader.load({
					key: AMAP_KEY,
					version: '2.0',
					plugins: AMAP_PLUGINS,
				});
			} catch (err) {
				console.error('[spa-map] 高德地图加载失败：', err);
				mapAvailable.value = false;
				locationStatus.value = '地图暂时没有加载成功';
				return;
			}

			mapAvailable.value = true;
			await new Promise((resolve) => setTimeout(resolve, 0)); // 等待容器渲染出高度

			map = new AMap.Map(mapContainer.value, {
				zoom: 12,
				center: [116.3974, 39.9093],
				scrollWheel: true,
				doubleClickZoom: true,
				viewMode: '2D',
			});
			map.addControl(new AMap.Scale());
			map.addControl(new AMap.ToolBar({ position: 'RB' }));

			geolocation = new AMap.Geolocation({
				enableHighAccuracy: true,
				timeout: 12000,
				showButton: false,
				showMarker: false,
				showCircle: false,
			});

			runCityLocation();
		};

		onMounted(() => {
			initMap();
		});

		onBeforeUnmount(() => {
			searchToken += 1;
			if (map) {
				map.destroy();
				map = null;
			}
		});

		return {
			mapContainer,
			mapAvailable,
			locating,
			searching,
			searchedOnce,
			locationStatus,
			locationTitle,
			locationHint,
			lastUpdated,
			userLocation,
			poiList,
			categories,
			formatDistance,
			locatePrecise,
			refreshNearby,
			focusPoi,
		};
	},
};
</script>

<style scoped>
* {
	box-sizing: border-box;
}

.spa-map-page {
	min-height: calc(100vh - 84px);
	background: #fbf6ef;
	color: #3f3b35;
	display: flex;
	flex-direction: column;
}

.map-hero {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	gap: 24px;
	padding: 28px 34px;
	background: linear-gradient(135deg, #fff8ef, #f4eadc);
	border-bottom: 1px solid rgba(174, 133, 91, 0.16);
}

.map-hero p {
	margin: 0 0 8px;
	color: #b47b4f;
	font-weight: 800;
}

.map-hero h1 {
	margin: 0 0 10px;
	color: #3f332b;
	font-size: 30px;
	letter-spacing: 0;
}

.map-hero span,
.soft-text,
.poi-card p,
.warm-contact p,
.final-note p,
.empty-box p {
	color: #75695e;
	line-height: 1.7;
}

.hero-note {
	flex: 0 0 auto;
	padding: 10px 14px;
	border-radius: 999px;
	background: #fff3df;
	color: #94623e;
	font-weight: 800;
	border: 1px solid rgba(174, 133, 91, 0.18);
}

.content {
	flex: 1;
	display: grid;
	grid-template-columns: minmax(0, 1.7fr) 430px;
	min-height: 620px;
}

.map-section {
	position: relative;
	min-height: 620px;
}

.amap-container {
	height: 100%;
	width: 100%;
	min-height: 620px;
	filter: sepia(0.12) saturate(0.86) brightness(1.02);
}

.map-fallback {
	position: absolute;
	inset: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 32px;
	background:
		radial-gradient(circle at 30% 20%, rgba(231, 200, 110, 0.12), transparent 45%),
		radial-gradient(circle at 75% 80%, rgba(201, 143, 92, 0.14), transparent 50%),
		#f7efe3;
}

.fallback-card {
	max-width: 560px;
	padding: 30px 32px;
	background: rgba(255, 255, 255, 0.95);
	border: 1px solid rgba(174, 133, 91, 0.16);
	border-radius: 12px;
	box-shadow: 0 16px 36px rgba(91, 70, 45, 0.12);
}

.fallback-card h2 {
	margin: 0 0 12px;
	color: #6d4c36;
	font-size: 22px;
}

.fallback-card p {
	color: #75695e;
	line-height: 1.8;
}

.fallback-steps {
	margin: 16px 0;
	padding: 14px 18px;
	background: #fff7ed;
	border: 1px dashed rgba(174, 133, 91, 0.3);
	border-radius: 8px;
}

.fallback-steps p {
	margin: 0 0 8px;
	color: #6d4c36;
}

.fallback-steps ol {
	margin: 0;
	padding-left: 20px;
	color: #75695e;
	line-height: 1.9;
}

.fallback-steps code {
	padding: 2px 6px;
	border-radius: 4px;
	background: #f4eadc;
	color: #8a5a37;
	font-size: 13px;
}

.fallback-soft {
	font-size: 13px;
	color: #9a8b7c !important;
}

.map-floating {
	position: absolute;
	left: 20px;
	right: 20px;
	bottom: 20px;
	z-index: 100;
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	gap: 16px;
	pointer-events: none;
}

.position-card,
.legend,
.poi-card,
.accuracy-box,
.warm-contact,
.empty-box {
	background: rgba(255, 255, 255, 0.94);
	border: 1px solid rgba(174, 133, 91, 0.14);
	border-radius: 8px;
	box-shadow: 0 12px 28px rgba(91, 70, 45, 0.1);
}

.position-card {
	max-width: 360px;
	padding: 14px 16px;
	display: grid;
	gap: 6px;
}

.position-card strong {
	color: #6d4c36;
	font-size: 16px;
}

.position-card span {
	color: #75695e;
	line-height: 1.6;
}

.legend {
	padding: 12px 14px;
	font-size: 13px;
}

.legend-item {
	display: flex;
	align-items: center;
	margin: 7px 0;
	color: #67594e;
}

.legend-icon {
	width: 18px;
	height: 18px;
	border-radius: 50%;
	margin-right: 9px;
	flex: 0 0 auto;
}

.icon-user {
	background: #c98f5c;
}

.icon-radius {
	background: rgba(201, 143, 92, 0.22);
	border: 1px solid #c98f5c;
}

.controls {
	background: #fffaf4;
	padding: 22px;
	overflow-y: auto;
	border-left: 1px solid rgba(174, 133, 91, 0.16);
	max-height: calc(100vh - 84px);
}

.control-section {
	margin-bottom: 22px;
	padding-bottom: 20px;
	border-bottom: 1px dashed rgba(174, 133, 91, 0.22);
}

.control-section h2 {
	margin: 0 0 12px;
	color: #7a5a43;
	font-size: 19px;
	letter-spacing: 0;
}

.btn {
	width: 100%;
	padding: 13px;
	margin: 8px 0;
	border: none;
	border-radius: 8px;
	background: #c98f5c;
	color: #ffffff;
	font-size: 15px;
	font-weight: 800;
	cursor: pointer;
	transition: 0.2s ease;
}

.btn:hover {
	transform: translateY(-1px);
}

.btn:disabled {
	cursor: not-allowed;
	opacity: 0.65;
	transform: none;
}

.btn-light {
	background: #e7c7a7;
	color: #5a3f2d;
}

.accuracy-box {
	margin-top: 10px;
	padding: 12px 14px;
	background: #fff7ed;
	color: #67594e;
}

.accuracy-box p {
	margin: 4px 0;
	line-height: 1.6;
}

.poi-list {
	display: grid;
	gap: 12px;
}

.poi-card {
	padding: 15px;
	cursor: pointer;
	transition: 0.2s ease;
}

.poi-card:hover {
	transform: translateY(-1px);
	box-shadow: 0 14px 30px rgba(91, 70, 45, 0.16);
	border-color: rgba(174, 133, 91, 0.3);
}

.resource-head {
	display: flex;
	align-items: center;
	gap: 10px;
}

.resource-head span {
	width: 14px;
	height: 14px;
	border-radius: 50%;
	flex: 0 0 auto;
	box-shadow: 0 0 0 4px rgba(201, 143, 92, 0.1);
}

.resource-head h3 {
	margin: 0;
	color: #604838;
	font-size: 16px;
	letter-spacing: 0;
	line-height: 1.4;
}

.poi-card p {
	margin: 6px 0 0;
	font-size: 13px;
}

.poi-cat {
	color: #9a7b5f !important;
	font-weight: 700;
}

.poi-addr,
.poi-tel {
	color: #75695e;
}

.empty-box {
	padding: 16px;
	background: #fff7ed;
}

.empty-box p {
	margin: 0;
}

.warm-contact {
	padding: 16px;
	background: #fff7ed;
}

.final-note {
	border-bottom: none;
}

footer {
	background: #7a5a43;
	color: #fff8ef;
	text-align: center;
	padding: 16px;
}

@media (max-width: 980px) {
	.map-hero,
	.map-floating {
		align-items: flex-start;
		flex-direction: column;
	}

	.content {
		grid-template-columns: 1fr;
	}

	.map-section,
	.amap-container {
		height: 480px;
		min-height: 480px;
	}

	.controls {
		border-left: none;
		max-height: none;
	}
}
</style>

<style>
/* 高德地图覆盖层样式（非 scoped，因 marker / InfoWindow 由高德渲染在组件 DOM 之外） */
.spa-user-marker {
	width: 26px;
	height: 26px;
	display: flex;
	align-items: center;
	justify-content: center;
}

.spa-user-marker span {
	width: 16px;
	height: 16px;
	background: #c98f5c;
	border: 3px solid #ffffff;
	border-radius: 50%;
	box-shadow: 0 2px 8px rgba(91, 70, 45, 0.35);
	animation: spa-pulse 2s infinite;
}

@keyframes spa-pulse {
	0% {
		box-shadow: 0 0 0 0 rgba(201, 143, 92, 0.55);
	}

	70% {
		box-shadow: 0 0 0 12px rgba(201, 143, 92, 0);
	}

	100% {
		box-shadow: 0 0 0 0 rgba(201, 143, 92, 0);
	}
}

.spa-poi-marker {
	width: 14px;
	height: 14px;
	border: 2px solid #ffffff;
	border-radius: 50%;
	box-shadow: 0 2px 6px rgba(91, 70, 45, 0.35);
	cursor: pointer;
}

.amap-info-content {
	border-radius: 10px !important;
	background: #fffaf4 !important;
	border: 1px solid rgba(174, 133, 91, 0.25) !important;
	box-shadow: 0 12px 28px rgba(91, 70, 45, 0.18) !important;
}

.spa-info {
	max-width: 280px;
	font-family: inherit;
}

.spa-info-title {
	display: flex;
	align-items: center;
	gap: 8px;
	margin-bottom: 6px;
	color: #604838;
	font-size: 15px;
}

.spa-info-dot {
	width: 12px;
	height: 12px;
	border-radius: 50%;
	flex: 0 0 auto;
}

.spa-info-line {
	margin: 4px 0;
	color: #75695e;
	font-size: 13px;
	line-height: 1.6;
}

.spa-info-actions {
	display: flex;
	align-items: center;
	gap: 10px;
	margin-top: 10px;
}

.spa-info-actions a {
	color: #b47b4f;
	font-size: 13px;
	font-weight: 700;
	text-decoration: none;
	border-bottom: 1px dashed rgba(180, 123, 79, 0.5);
}

.spa-info-copy {
	border: none;
	border-radius: 6px;
	padding: 6px 12px;
	background: #c98f5c;
	color: #ffffff;
	font-size: 13px;
	font-weight: 700;
	cursor: pointer;
}

.amap-info-sharp {
	border-top-color: #fffaf4 !important;
}
</style>
