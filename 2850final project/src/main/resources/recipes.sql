CREATE TABLE recipes (
    id SERIAL PRIMARY KEY,
    created_by INT NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    prep_time_minutes INT NOT NULL,
    cook_time_minutes INT NOT NULL,
    servings INT NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

INSERT INTO recipes (
    created_by,
    title,
    description,
    prep_time_minutes,
    cook_time_minutes,
    servings,
    difficulty,
    image_url,
    created_at,
    updated_at
)
VALUES
(
    1,
    'Grilled Chicken Salad',
    'Fresh mixed greens with grilled chicken breast, cherry tomatoes, cucumber, and olive oil dressing.',
    10,
    15,
    2,
    'easy',
    NULL,
    NOW(),
    NULL
),
(
    1,
    'Overnight Oats Bowl',
    'Nutritious oats soaked overnight with yogurt, chia seeds, banana, and almonds.',
    10,
    0,
    1,
    'easy',
    NULL,
    NOW(),
    NULL
),
(
    1,
    'Grilled Salmon with Veggies',
    'Omega-3 rich salmon served with roasted broccoli and sweet potato.',
    15,
    25,
    2,
    'medium',
    NULL,
    NOW(),
    NULL
);
```
