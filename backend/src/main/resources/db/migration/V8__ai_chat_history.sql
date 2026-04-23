CREATE TABLE IF NOT EXISTS ai_chat_histories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    prompt TEXT NOT NULL,
    response_summary TEXT NOT NULL,
    context_latitude NUMERIC(10,7),
    context_longitude NUMERIC(10,7),
    weather_h3_index_res8 VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ai_chat_histories_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS ai_chat_histories_user_created_idx ON ai_chat_histories(user_id, created_at DESC);
