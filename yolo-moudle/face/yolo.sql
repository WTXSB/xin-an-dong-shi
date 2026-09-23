/*
 Navicat Premium Data Transfer

 Source Server         : try
 Source Server Type    : MySQL
 Source Server Version : 80034
 Source Host           : 127.0.0.1:3306
 Source Schema         : yolo

 Target Server Type    : MySQL
 Target Server Version : 80034
 File Encoding         : 65001

 Date: 02/12/2025 13:59:41
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for camerarecords
-- ----------------------------
DROP TABLE IF EXISTS `camerarecords`;
CREATE TABLE `camerarecords`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `weight` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `conf` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `start_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `out_video` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `kind` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of camerarecords
-- ----------------------------

-- ----------------------------
-- Table structure for emotionrecords
-- ----------------------------
DROP TABLE IF EXISTS `emotionrecords`;
CREATE TABLE `emotionrecords`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `emotion_kind` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `txt` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `start_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of emotionrecords
-- ----------------------------
INSERT INTO `emotionrecords` VALUES (1, '悲伤', '吃火锅', '2025-11-20 18:46:39');
INSERT INTO `emotionrecords` VALUES (2, '悲伤', '不及格', '2025-11-19 19:54:37');
INSERT INTO `emotionrecords` VALUES (3, '生气', '吵架', '2025-11-18 19:54:37');
INSERT INTO `emotionrecords` VALUES (4, '生气', NULL, '2025-11-20 19:54:37');
INSERT INTO `emotionrecords` VALUES (5, '悲伤', '吵架', '2025-11-17 19:54:37');
INSERT INTO `emotionrecords` VALUES (6, '悲伤', '1', '2025-11-21 16:40:39');
INSERT INTO `emotionrecords` VALUES (7, '悲伤', '今天吃了火锅', '2025-11-21 16:41:12');
INSERT INTO `emotionrecords` VALUES (8, '悲伤', '1', '2025-11-21 16:41:12');
INSERT INTO `emotionrecords` VALUES (9, '生气', NULL, '2025-11-21 16:41:12');
INSERT INTO `emotionrecords` VALUES (10, '中性', NULL, '2025-11-21 16:41:12');

-- ----------------------------
-- Table structure for imgrecords
-- ----------------------------
DROP TABLE IF EXISTS `imgrecords`;
CREATE TABLE `imgrecords`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `input_img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `out_img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `confidence` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `all_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `conf` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `weight` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `start_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `label` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `kind` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 877 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of imgrecords
-- ----------------------------
INSERT INTO `imgrecords` VALUES (842, 'http://localhost:9999/files/46d16164a1ac4f0abaa923b9e194d689_50c44178b111966f827f60a686641f4c49ea5571.jpg', 'http://localhost:9999/files/946696504c0e4cd19d4c90e609e82790_result.jpg', '[\"96.21%\", \"95.87%\"]', '2.505秒', '0.35', 'plate_best.pt', 'admin', '2025-07-20 21:09:44', '[\"\\u6d5988888\", \"\\u76963A8888\"]', 'plate');
INSERT INTO `imgrecords` VALUES (843, 'http://localhost:9999/files/34ba5a681bbc49b2bd6ee5bd2d7b3ff0_50c44178b111966f827f60a686641f4c49ea5571.jpg', 'http://localhost:9999/files/5a3c9bbaae5c4df6bb579b6191988878_result.jpg', '[\"96.21%\", \"95.87%\"]', '0.177秒', '0.54', 'plate_best.pt', 'admin', '2025-07-20 21:11:20', '[\"\\u6d5988888\", \"\\u76963A8888\"]', 'plate');
INSERT INTO `imgrecords` VALUES (844, 'http://localhost:9999/files/5877b1cf88a744adba774ce6a8e4483d_53c1aa3f58544a9999371e9720bf60ef.jpg', 'http://localhost:9999/files/d868e4578b03408b9f835c3c7cc40a4e_result.jpg', '[\"96.72%\"]', '0.316秒', '0.54', 'plate_best.pt', 'admin', '2025-07-20 21:12:11', '[\"\\u82cf800137C\"]', 'plate');
INSERT INTO `imgrecords` VALUES (845, 'http://localhost:9999/files/c54f38efb2ec45f28201234c5fc3b2fd_u=2357725595,1351850669&fm=253&fmt=auto&app=138&f=JPEG.webp', 'http://localhost:9999/files/f26e126608e24d92b59ba0322852d1d9_result.jpg', '[\"98.39%\"]', '0.164秒', '0.54', 'plate_best.pt', 'admin', '2025-07-20 21:12:44', '[\"\\u8fbdAR000P\"]', 'plate');
INSERT INTO `imgrecords` VALUES (846, 'http://localhost:9999/files/6922e1b5c5d446b5a1fbafa6dc98f927_bf7f-iikmuth7946495.jpg', 'http://localhost:9999/files/f19a2a97921a46f192b3890ed6869839_result.jpg', '[\"98.18%\"]', '2.256秒', '0.26', 'plate_best.pt', 'admin', '2025-07-21 15:49:07', '[\"\\u82cfF66666\"]', 'plate');
INSERT INTO `imgrecords` VALUES (847, 'http://localhost:9999/files/ed8f0dda3d42413793030522128ae20b_0089224137931-92_85-368&433_515&496-516&498_369&484_373&433_520&447-0_0_22_30_1_29_33-134-36.jpg', 'http://localhost:9999/files/1b05cff16d5e4b4587009c553cbbf44e_result.jpg', '[\"98.36%\"]', '0.190秒', '0.42', 'plate_best.pt', 'admin', '2025-07-21 18:05:02', '[\"\\u7696AY6B59\"]', 'plate');
INSERT INTO `imgrecords` VALUES (848, 'http://localhost:9999/files/ed8f0dda3d42413793030522128ae20b_0089224137931-92_85-368&433_515&496-516&498_369&484_373&433_520&447-0_0_22_30_1_29_33-134-36.jpg', 'http://localhost:9999/files/6022d4903eb04874aa07857646ee0ce0_result.jpg', '[\"98.36%\"]', '0.152秒', '0.42', 'plate_best.pt', 'admin', '2025-07-21 18:05:37', '[\"\\u7696AY6B59\"]', 'plate');
INSERT INTO `imgrecords` VALUES (849, 'http://localhost:9999/files/16f586ce129345cea72eb8529d91fd75_0089224137931-92_85-368&433_515&496-516&498_369&484_373&433_520&447-0_0_22_30_1_29_33-134-36.jpg', 'http://localhost:9999/files/9e704e7bb87c4875976eec9c6f942a5c_result.jpg', '[\"98.36%\"]', '0.141秒', '0.42', 'plate_best.pt', 'admin', '2025-07-21 18:05:50', '[\"\\u7696AY6B59\"]', 'plate');
INSERT INTO `imgrecords` VALUES (850, 'http://localhost:9999/files/16f586ce129345cea72eb8529d91fd75_0089224137931-92_85-368&433_515&496-516&498_369&484_373&433_520&447-0_0_22_30_1_29_33-134-36.jpg', 'http://localhost:9999/files/9adcda1fde6d4ca3b75091b6047d9e9a_result.jpg', '[\"98.36%\"]', '0.141秒', '0.42', 'plate_best.pt', 'admin', '2025-07-21 18:13:58', '[\"\\u7696AY6B59\"]', 'plate');
INSERT INTO `imgrecords` VALUES (851, 'http://localhost:9999/files/f372aee9b24848c8962825c33409eb48_0089224137931-92_85-368&433_515&496-516&498_369&484_373&433_520&447-0_0_22_30_1_29_33-134-36.jpg', 'http://localhost:9999/files/dd93f0c4a35b42c39f03207a9e3a91eb_result.jpg', '[\"98.36%\"]', '3.719秒', '0.33', 'plate_best.pt', 'admin', '2025-07-22 13:48:55', '[\"\\u7696AY6B59\"]', 'plate');
INSERT INTO `imgrecords` VALUES (852, 'http://localhost:9999/files/f372aee9b24848c8962825c33409eb48_0089224137931-92_85-368&433_515&496-516&498_369&484_373&433_520&447-0_0_22_30_1_29_33-134-36.jpg', 'http://localhost:9999/files/b2de67bdbf0f42f4a5f877ec28e82de0_result.jpg', '[\"98.36%\"]', '0.470秒', '0.33', 'plate_best.pt', 'admin', '2025-07-22 13:48:59', '[\"\\u7696AY6B59\"]', 'plate');
INSERT INTO `imgrecords` VALUES (853, 'http://localhost:9999/files/c0d6f768d6164a0caa4d618bc474f094_bf7f-iikmuth7946495.jpg', 'http://localhost:9999/files/b74cac4106a5440eba16503668710f54_result.jpg', '[\"98.18%\"]', '0.559秒', '0.24', 'plate_best.pt', 'admin', '2025-07-22 13:51:09', '[\"\\u82cfF66666\"]', 'plate');
INSERT INTO `imgrecords` VALUES (854, 'http://localhost:9999/files/5acfe621edca43c386b87479b6eb0caf_0089224137931-92_85-368&433_515&496-516&498_369&484_373&433_520&447-0_0_22_30_1_29_33-134-36.jpg', 'http://localhost:9999/files/eff57a3c009b496b85e0720f3af04e45_result.jpg', '[\"98.36%\"]', '0.481秒', '0.38', 'plate_best.pt', 'admin', '2025-07-22 14:14:43', '[\"\\u7696AY6B59\"]', 'plate');
INSERT INTO `imgrecords` VALUES (855, 'http://localhost:9999/files/5acfe621edca43c386b87479b6eb0caf_0089224137931-92_85-368&433_515&496-516&498_369&484_373&433_520&447-0_0_22_30_1_29_33-134-36.jpg', 'http://localhost:9999/files/c36631c334844260816a7d894edfdeb2_result.jpg', '[\"98.36%\"]', '0.442秒', '0.38', 'plate_best.pt', 'admin', '2025-07-22 14:15:21', '[\"\\u7696AY6B59\"]', 'plate');
INSERT INTO `imgrecords` VALUES (857, 'http://localhost:9999/files/a28ba659ce8a432993e5a06fc539d004_2c2ef20b965c4e89ba363ba60641f5fc.webp', 'http://localhost:9999/files/220316203442499694ddf92d4ff20bc4_result.jpg', '[\"98.27%\"]', '2.971秒', '0.3', 'plate_best.pt', 'admin', '2025-10-24 11:09:39', '[\"\\u7696J59P09\"]', 'plate');
INSERT INTO `imgrecords` VALUES (863, 'http://localhost:9999/files/afdb4778c544409e9f40f9a3c03a547f_2c2ef20b965c4e89ba363ba60641f5fc.webp', 'http://localhost:9999/files/1e540140a5bb411e880132e6c1f83271_result.jpg', '[\"98.27%\"]', '0.194秒', '0.22', 'plate_best.pt', 'admin', '2025-10-24 11:15:03', '[\"\\u7696J59P09\"]', 'plate');
INSERT INTO `imgrecords` VALUES (864, 'http://localhost:9999/files/a796883dc4784ee2af85b98743fa0d7e_2c2ef20b965c4e89ba363ba60641f5fc.webp', 'http://localhost:9999/files/f384132e4da04ad4a58c48d227f2cb31_result.jpg', '[\"98.27%\"]', '0.200秒', '0.24', 'plate_best.pt', 'admin', '2025-10-24 11:23:10', '[\"\\u7696J59P09\"]', 'plate');
INSERT INTO `imgrecords` VALUES (865, 'http://localhost:9999/files/b076f0607e9b46db8943303af5a32c38_3.jpg', 'http://localhost:9999/files/4d3bdf5309154a6f9931af2a3ce7063b_result.jpg', '[\"0.86\"]', '1.661秒', '0.39', 'emotion.pt', 'admin', '2025-11-16 20:28:41', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (866, 'http://localhost:9999/files/b076f0607e9b46db8943303af5a32c38_3.jpg', 'http://localhost:9999/files/f344ac8ee3d1486e981f8707478136dc_result.jpg', '[\"0.86\"]', '0.172秒', '0.39', 'emotion.pt', 'admin', '2025-11-16 20:30:25', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (867, 'http://localhost:9999/files/396e9972413a474f92be79ea2494f167_3.jpg', 'http://localhost:9999/files/2cc836a43211462e8e7fa4101e081ee7_result.jpg', '[\"0.86\"]', '2.222秒', '0.17', 'emotion.pt', 'admin', '2025-11-18 16:24:19', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (868, 'http://localhost:9999/files/396e9972413a474f92be79ea2494f167_3.jpg', 'http://localhost:9999/files/5b667d1b85bc4502bc555f85e14360cf_result.jpg', '[\"0.86\"]', '0.166秒', '0.17', 'emotion.pt', 'admin', '2025-11-18 16:24:54', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (869, 'http://localhost:9999/files/07378234e23643f09a75943bd4e5387c_3.jpg', 'http://localhost:9999/files/0886bda85c854617906f5e18422e8648_result.jpg', '[\"0.86\"]', '0.180秒', '0.09', 'emotion.pt', 'admin', '2025-11-18 16:28:23', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (870, 'http://localhost:9999/files/c1e5426494164692a646e57f076c976e_3.jpg', 'http://localhost:9999/files/1e31cb6c59824f00aa5bc21c3b717f79_result.jpg', '[\"0.86\"]', '0.168秒', '0.08', 'emotion.pt', 'admin', '2025-11-18 16:33:22', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (871, 'http://localhost:9999/files/0a3364e435e5401f865cdc46015e389f_3.jpg', 'http://localhost:9999/files/6a3621c145bc413f805932bf1ed21168_result.jpg', '[\"0.86\"]', '0.157秒', '0.13', 'emotion.pt', 'admin', '2025-11-18 16:33:56', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (872, 'http://localhost:9999/files/dfbba46794144caa9ac71bb603bc4618_3.jpg', 'http://localhost:9999/files/98d395a3369941e689fd309c7c747df1_result.jpg', '[\"0.86\"]', '0.191秒', '0.17', 'emotion.pt', 'admin', '2025-11-18 16:34:49', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (873, 'http://localhost:9999/files/797c767b1e3043db83087fcd0fbe275d_3.jpg', 'http://localhost:9999/files/ff2dc9020b5b4f2a9db65a498b6bde92_result.jpg', '[\"0.86\"]', '0.179秒', '0.17', 'emotion.pt', 'admin', '2025-11-18 16:37:18', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (874, 'http://localhost:9999/files/8d6037591c8642c0b7ce8c287bb5fedd_3.jpg', 'http://localhost:9999/files/1a369734252d473db6551e898153ffbb_result.jpg', '[\"0.86\"]', '0.153秒', '0.14', 'emotion.pt', 'admin', '2025-11-18 16:39:31', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (875, 'http://localhost:9999/files/d9b45f49c9874d549a5d34cdadaa084e_3.jpg', 'http://localhost:9999/files/934029e938ae441eb25d27072a566b02_result.jpg', '[\"0.86\"]', 'null', '0.15', 'emotion.pt', 'admin', '2025-11-20 18:44:06', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (876, 'http://localhost:9999/files/38f069d8f8534af9a76cfac394fc8d18_3.jpg', 'http://localhost:9999/files/b8e684efe89c4c6680ac0192b9c4f365_result.jpg', '[\"0.86\"]', 'null', '0.18', 'emotion.pt', 'admin', '2025-11-20 18:46:39', '[\"happy\"]', 'emotion');
INSERT INTO `imgrecords` VALUES (877, 'http://localhost:9999/files/6a65bd47b804458994ee5cc3c16028e4_3.jpg', 'http://localhost:9999/files/f4ef959d0f5e4380b11a5368c2c54494_result.jpg', '[\"0.86\"]', 'null', '0.18', 'emotion.pt', 'admin', '2025-11-21 16:40:39', '[\"happy\"]', 'emotion');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sex` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tel` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 45 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Table \'.\\demo\\user\' is marked as crashed and should be repaired' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', 'admin', '李四', '女', '123@qq.com', '1234567891', 'admin', 'http://localhost:9999/files/e128587845f24efca6fb1be0ac990cf3_8888e6970ed9fc1a1acea8773c831ad4.jpeg', NULL);
INSERT INTO `user` VALUES (2, 'user', 'user', '张三', '男', '123@qq.com', '1234567889', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (3, 'user001', 'pass123', '王伟', '男', 'wangwei@example.com', '13800138001', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (4, 'user002', 'pass456', '李娜', '女', 'lina@example.com', '13900139002', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (5, 'user003', 'pass789', '张强', '男', 'zhangqiang@example.com', '13700137003', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (6, 'user004', 'pass101', '刘芳', '女', 'liufang@example.com', '13600136004', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (7, 'user005', 'pass202', '陈明', '男', 'chenming@example.com', '13500135005', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (8, 'user006', 'pass303', '杨丽', '女', 'yangli@example.com', '13400134006', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (9, 'user007', 'pass404', '赵勇', '男', 'zhaoyong@example.com', '13300133007', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (10, 'user008', 'pass505', '周雪', '女', 'zhouxue@example.com', '13200132008', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (11, 'user009', 'pass606', '吴刚', '男', 'wugang@example.com', '13100131009', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (12, 'user010', 'pass707', '郑梅', '女', 'zhengmei@example.com', '13000130010', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (13, 'user011', 'pass808', '孙浩', '男', 'sunhao@example.com', '15900159011', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (14, 'user012', 'pass909', '朱琳', '女', 'zhulin@example.com', '15800158012', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (15, 'user013', 'pass010', '胡强', '男', 'huqiang@example.com', '15700157013', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (16, 'user014', 'pass111', '林静', '女', 'linjing@example.com', '15600156014', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (17, 'user015', 'pass212', '徐杰', '男', 'xujie@example.com', '15500155015', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (18, 'user016', 'pass313', '高敏', '女', 'gaomin@example.com', '15400154016', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (19, 'user017', 'pass414', '马超', '男', 'machao@example.com', '15300153017', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (20, 'user018', 'pass515', '黄娟', '女', 'huangjuan@example.com', '15200152018', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (21, 'user019', 'pass616', '谢军', '男', 'xiejun@example.com', '15100151019', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (22, 'user020', 'pass717', '韩雪', '女', 'hanxue@example.com', '15000150020', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (23, 'user021', 'pass818', '董伟', '男', 'dongwei@example.com', '18800188021', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (24, 'user022', 'pass919', '梁燕', '女', 'liangyan@example.com', '18900189022', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (25, 'user023', 'pass020', '宋强', '男', 'songqiang@example.com', '18700187023', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (26, 'user024', 'pass121', '唐丽', '女', 'tangli@example.com', '18600186024', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (27, 'user025', 'pass222', '冯刚', '男', 'fenggang@example.com', '18500185025', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (28, 'user026', 'pass323', '于娜', '女', 'yuna@example.com', '18400184026', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (29, 'user027', 'pass424', '邓杰', '男', 'dengjie@example.com', '18300183027', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (30, 'user028', 'pass525', '曹敏', '女', 'caomin@example.com', '18200182028', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (31, 'user029', 'pass626', '彭伟', '男', 'pengwei@example.com', '18100181029', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (32, 'user030', 'pass727', '潘雪', '女', 'panxue@example.com', '18000180030', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (33, 'user031', 'pass828', '钟强', '男', 'zhongqiang@example.com', '17700177031', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (34, 'user032', 'pass929', '田静', '女', 'tianjing@example.com', '17800178032', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (35, 'user033', 'pass030', '姜军', '男', 'jiangjun@example.com', '17600176033', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (36, 'user034', 'pass131', '崔丽', '女', 'cuili@example.com', '17500175034', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (37, 'user035', 'pass232', '谭刚', '男', 'tangang@example.com', '17400174035', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (38, 'user036', 'pass333', '陆敏', '女', 'lumin@example.com', '17300173036', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (39, 'user037', 'pass434', '白杰', '男', 'baijie@example.com', '17200172037', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (40, 'user038', 'pass535', '江燕', '女', 'jiangyan@example.com', '17100171038', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (41, 'user039', 'pass636', '丁伟', '男', 'dingwei@example.co', '17000170039', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (42, 'user040', 'pass737', '方雪', '男', 'fangxue@example.com', '19900199040', 'common', 'http://localhost:9999/files/15e09359b18544dbb65dc9dadb833abd_人.png', NULL);
INSERT INTO `user` VALUES (43, 'try111', '123456', 'try666', '男', '2783309461@qq.com', '1111', 'common', NULL, NULL);

-- ----------------------------
-- Table structure for videorecords
-- ----------------------------
DROP TABLE IF EXISTS `videorecords`;
CREATE TABLE `videorecords`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `input_video` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `out_video` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `start_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `conf` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `weight` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `kind` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 138 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of videorecords
-- ----------------------------
INSERT INTO `videorecords` VALUES (120, 'http://localhost:9999/files/92f6238e296c417d9620c4e046a1d744_car.mp4', 'http://localhost:9999/files/6e52df97e4f641bda1fd025ce17a4880_output.mp4', 'admin', '2025-07-19 22:45:39', '0.5', 'plate_best.pt', 'plate');
INSERT INTO `videorecords` VALUES (121, 'http://localhost:9999/files/d4caa996398b4bf4aa262f0ab766d04f_car.mp4', 'http://localhost:9999/files/739d3b4d1460430d8cc9f0663a1221c8_output.mp4', 'admin', '2025-07-20 21:13:08', '0.5', 'plate_best.pt', 'plate');
INSERT INTO `videorecords` VALUES (122, 'http://localhost:9999/files/0be6af01d69746c9893d502ac17c5ae9_car.mp4', 'http://localhost:9999/files/62a138fcc8d8422aa55ed50f85a56530_output.mp4', 'admin', '2025-07-20 22:27:08', '0.5', 'plate_best.pt', 'plate');
INSERT INTO `videorecords` VALUES (123, 'http://localhost:9999/files/c415b90fc586406c976a807d8b1bd1a2_car.mp4', 'http://localhost:9999/files/4f3c33fd175d4caea65040252ead223d_output.mp4', 'admin', '2025-07-20 22:30:47', '0.5', 'plate_best.pt', 'plate');
INSERT INTO `videorecords` VALUES (124, 'http://localhost:9999/files/f9621210579b4194bc192e00b08561a3_car.mp4', 'http://localhost:9999/files/abbd67728c9141b6baba41c26013f371_output.mp4', 'admin', '2025-07-22 13:51:54', '0.5', 'plate_best.pt', 'plate');
INSERT INTO `videorecords` VALUES (125, 'http://localhost:9999/files/b36fa8c3dc95421c9d6ef30d0daf8881_car.mp4', 'http://localhost:9999/files/194f238f16694c969df809928dc15b69_output.mp4', 'admin', '2025-07-22 14:15:53', '0.5', 'plate_best.pt', 'plate');
INSERT INTO `videorecords` VALUES (126, 'http://localhost:9999/files/f3791e1d07b74c579316263d8c38ee00_car.mp4', 'http://localhost:9999/files/3e6ae864b17e4e46b68197f99db9f60c_output.mp4', 'admin', '2025-07-22 14:17:14', '0.5', 'plate_best.pt', 'plate');
INSERT INTO `videorecords` VALUES (127, 'http://localhost:9999/files/069ff972307a4fcdbd2536549f5f5db4_car.mp4', 'http://localhost:9999/files/bb9321648cce403e8185f0ee0889c428_output.mp4', 'admin', '2025-10-24 11:24:54', '0.5', 'plate_best.pt', 'plate');
INSERT INTO `videorecords` VALUES (128, 'http://localhost:9999/files/55e85e4c34eb43568571d06e0d167617_car.mp4', 'http://localhost:9999/files/40c75d8882b1469989baaf564471616f_output.mp4', 'admin', '2025-10-24 11:27:25', '0.5', 'plate_best.pt', 'plate');
INSERT INTO `videorecords` VALUES (129, 'http://localhost:9999/files/1b2b2fc1e907446ebad0842bbdcbd193_QQ20251119-132934.mp4', 'http://localhost:9999/files/0c97e35f192b4cd8bc4ec921ab85ff7c_output.mp4', 'admin', '2025-11-19 13:39:05', '0.19', 'emotion.pt', 'emotion');
INSERT INTO `videorecords` VALUES (130, 'http://localhost:9999/files/fb12c89b5ab042bba25756c7e11f7941_QQ20251119-132934.mp4', 'http://localhost:9999/files/e39e1f7c80d54de7908d68e4ad5be59c_output.mp4', 'admin', '2025-11-19 13:51:27', '0.27', 'emotion.pt', 'emotion');
INSERT INTO `videorecords` VALUES (131, 'http://localhost:9999/files/8c174b248acb4a909e8230dcf0a644c4_QQ20251119-135921.mp4', 'http://localhost:9999/files/a9dad70b89d6409a96242788e9abbd53_output.mp4', 'admin', '2025-11-19 14:01:15', '0.27', 'emotion.pt', 'emotion');
INSERT INTO `videorecords` VALUES (132, 'http://localhost:9999/files/26c50a6a847f465aa0cff6160aa07139_QQ20251119-135921.mp4', 'http://localhost:9999/files/b713988175a7450e86257d3ffd2b2253_output.mp4', 'admin', '2025-11-19 14:06:17', '0.15', 'emotion.pt', 'emotion');
INSERT INTO `videorecords` VALUES (133, 'http://localhost:9999/files/888a8c51e17d4144b1bdb8237d9ec38d_QQ20251119-135921.mp4', 'http://localhost:9999/files/8c058b76f75a4dfa83ab5f9230da0459_output.mp4', 'admin', '2025-11-19 14:18:08', '0.15', 'emotion.pt', 'emotion');
INSERT INTO `videorecords` VALUES (134, 'http://localhost:9999/files/a5f2d807d67f445fb0418cd7eae20ccb_QQ20251119-14146.mp4', 'http://localhost:9999/files/6cbd09d1b8404d1ab42f395947ccd06a_output.mp4', 'admin', '2025-11-19 14:18:45', '0.15', 'emotion.pt', 'emotion');
INSERT INTO `videorecords` VALUES (135, 'http://localhost:9999/files/984b00c7e25b47cea3efb11bd36caab4_QQ20251119-14146.mp4', 'http://localhost:9999/files/1df0257f37f0484794568bdd23ef0d79_output.mp4', 'admin', '2025-11-19 14:19:21', '0.15', 'emotion.pt', 'emotion');
INSERT INTO `videorecords` VALUES (136, 'http://localhost:9999/files/4cefcb7d37c04e30af07d997e0d59c46_QQ20251119-143635.mp4', 'http://localhost:9999/files/41ca262118cc4230901955c3577eee54_output.mp4', 'admin', '2025-11-19 14:37:11', '0.15', 'emotion.pt', 'emotion');
INSERT INTO `videorecords` VALUES (137, 'http://localhost:9999/files/af4f8489aa0d4a53b2ff4ff821115670_QQ20251119-143635.mp4', 'http://localhost:9999/files/ec5eac50e5c84810a894cc0b6d84c64d_output.mp4', 'admin', '2025-11-20 19:54:37', '0.24', 'emotion.pt', 'emotion');
INSERT INTO `videorecords` VALUES (138, 'http://localhost:9999/files/8657c8f19aeb4eb98d9af028e20efc2c_QQ20251119-143635.mp4', 'http://localhost:9999/files/b56f749c24ae4137b72869a48d29e6ce_output.mp4', 'admin', '2025-11-21 16:41:12', '0.19', 'emotion.pt', 'emotion');

SET FOREIGN_KEY_CHECKS = 1;
