CREATE TABLE users
(
    user_name UUID PRIMARY KEY,
    name TEXT,
    email TEXT,
    gender TEXT,
    zip TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    modified_at TIMESTAMPTZ
);

CREATE TABLE apparel
(
    apparel_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_name UUID REFERENCES users (user_name),
    category TEXT,
    color TEXT,
    brand TEXT,
    image_url TEXT NOT NULL,
    active BOOLEAN     DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT now(),
    modified_at TIMESTAMPTZ
);

CREATE INDEX idx_apparel_user ON apparel (user_name);

CREATE TABLE apparel_analysis
(
    analysis_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    apparel_id BIGINT REFERENCES apparel (apparel_id),
    ai_description JSONB,
    model_version TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    modified_at TIMESTAMPTZ
);

CREATE INDEX idx_analysis_apparel ON apparel_analysis (apparel_id);

CREATE TABLE recommendations
(
    recommendation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_name UUID REFERENCES users (user_name),
    context JSONB,
    outfits JSONB,
    model_version TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    modified_at TIMESTAMPTZ
);

CREATE INDEX idx_recommendations_user ON recommendations (user_name);

CREATE TABLE recommendation_feedback
(
    feedback_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recommendation_id BIGINT REFERENCES recommendations (recommendation_id),
    feedback JSONB,
    created_at TIMESTAMPTZ DEFAULT now(),
    modified_at TIMESTAMPTZ
);

CREATE INDEX idx_feedback_recommendation ON recommendation_feedback (recommendation_id);

CREATE TABLE login_history
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_name UUID REFERENCES users (user_name),
    login_time TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_login_user ON login_history (user_name);

CREATE TABLE category
(
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT,
    display_order INT,
    created_at TIMESTAMPTZ DEFAULT now()
);