-- Align term_word.status with translate_state (0/1/2/3).
-- Legacy: approved|pending|deprecated → 3|1|2

UPDATE term_word SET status = '3' WHERE status = 'approved';
UPDATE term_word SET status = '1' WHERE status = 'pending';
UPDATE term_word SET status = '2' WHERE status = 'deprecated';
