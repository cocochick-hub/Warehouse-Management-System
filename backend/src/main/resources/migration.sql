-- ============================================================================
-- WMS 浠撳簱绠＄悊绯荤粺 - MySQL 鏁版嵁搴撹縼绉?SQL
-- 璇存槑锛氫负宸茬粡瀛樺湪鐨?MySQL 鏁版嵁搴撴坊鍔爌hone 瀛楁
-- ============================================================================

-- 涓篠ysUser 琛ㄦ坊鍔犺仈绯荤數璇濆瓧孌?
ALTER TABLE sys_user ADD COLUMN phone VARCHAR(30) DEFAULT NULL COMMENT '鑱旂郴鐢佃瘽' AFTER avatar;

-- 涓哄凡鏈夌敤鎴疯缃粯璁ょ數璇濆彿鐮?
UPDATE sys_user SET phone = '13800000001' WHERE username = 'admin' AND phone IS NULL;
UPDATE sys_user SET phone = '13800000002' WHERE username = 'operator' AND phone IS NULL;
UPDATE sys_user SET phone = '13800000003' WHERE username = 'manager' AND phone IS NULL;
