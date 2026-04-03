-- Fix incorrect H3 indexes in previously seeded restaurant records so nearby search can match seeded locations

UPDATE restaurants
SET h3_index_res9 = CASE id
    WHEN 'a0a0a0a0-0001-0001-0001-a0a0a0a0a001' THEN '8965b5662bbffff'
    WHEN 'a0a0a0a0-0002-0002-0002-a0a0a0a0a002' THEN '8965b564543ffff'
    WHEN 'a0a0a0a0-0003-0003-0003-a0a0a0a0a003' THEN '8965b574813ffff'
    WHEN '16161616-1616-1616-1616-161616161616' THEN '8965b5662a3ffff'
    ELSE h3_index_res9
END
WHERE id IN (
    'a0a0a0a0-0001-0001-0001-a0a0a0a0a001',
    'a0a0a0a0-0002-0002-0002-a0a0a0a0a002',
    'a0a0a0a0-0003-0003-0003-a0a0a0a0a003',
    '16161616-1616-1616-1616-161616161616'
);
