-- 原子自增计数并管理过期时间。
-- 替代「INCR 后判断首次再 EXPIRE」的两步写法：两步之间有竞态窗口，
-- 且 EXPIRE 一旦失败，计数将永不过期（登录限流会把用户永久锁死）。
-- KEYS[1] 计数 key
-- ARGV[1] 过期秒数
-- ARGV[2] 是否每次自增都续期：'1' = 滑动窗口（每次自增都续期），其余值 = 固定窗口（仅首次自增时设置）
-- 返回：自增后的计数值
local count = redis.call('INCR', KEYS[1])
if count == 1 or ARGV[2] == '1' then
  redis.call('EXPIRE', KEYS[1], ARGV[1])
end
return count
