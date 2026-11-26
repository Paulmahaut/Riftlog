-- ============================================
-- V4 — Cards are identified by their Riftcodex id.
--
-- riftbound_id is not unique in Riftcodex: "(Metal)" printings share the id
-- of the base card, and some cards are listed twice. Nothing references the
-- cards table yet (no deck or match exists on any branch), so it is emptied
-- and refilled by the importer on the next start.
-- ============================================

DELETE FROM cards;

ALTER TABLE cards DROP CONSTRAINT uk_cards_riftbound_id;

ALTER TABLE cards ADD COLUMN riftcodex_id VARCHAR(64) NOT NULL;

ALTER TABLE cards ADD CONSTRAINT uk_cards_riftcodex_id UNIQUE (riftcodex_id);

CREATE INDEX idx_cards_riftbound_id ON cards (riftbound_id);
