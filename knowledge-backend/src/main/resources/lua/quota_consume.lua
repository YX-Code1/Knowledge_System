-- 原子「校验 + 扣减」配额，杜绝并发下「读-判-扣」竞态导致的超扣。
-- KEYS[1] 配额 key（key 自带日期，跨天自然清零）
-- ARGV[1] 本次消耗量
-- ARGV[2] 配额上限
-- ARGV[3] 过期秒数（仅首次写入时设置，作为兜底回收，正常靠日期跨天重置）
-- 返回：扣减后的已用量；配额不足返回 -1（不写入）
local used = tonumber(redis.call('GET', KEYS[1]) or '0')
if used + tonumber(ARGV[1]) > tonumber(ARGV[2]) then
  return -1
end
local total = redis.call('INCRBY', KEYS[1], ARGV[1])
if total == tonumber(ARGV[1]) then
  redis.call('EXPIRE', KEYS[1], ARGV[3])
end
return total
