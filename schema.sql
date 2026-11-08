CREATE TABLE IF NOT EXISTS books (
    book_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    isbn text NOT NULL UNIQUE CHECK (length(trim(isbn)) > 0),
    title text NOT NULL CHECK (length(trim(title)) > 0),
    author text NOT NULL CHECK (length(trim(author)) > 0),
    genre text NOT NULL DEFAULT '',
    published_year integer NOT NULL CHECK (published_year BETWEEN 1 AND 9999),
    total_copies integer NOT NULL CHECK (total_copies >= 0),
    available_copies integer NOT NULL CHECK (available_copies BETWEEN 0 AND total_copies)
);
CREATE TABLE IF NOT EXISTS members (
    member_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_code text NOT NULL UNIQUE,
    name text NOT NULL CHECK (length(trim(name)) > 0),
    email text NOT NULL,
    phone text NOT NULL,
    membership_date date NOT NULL DEFAULT CURRENT_DATE,
    max_books_allowed integer NOT NULL DEFAULT 3 CHECK (max_books_allowed > 0),
    active_status boolean NOT NULL DEFAULT true
);
CREATE TABLE IF NOT EXISTS loans (
    loan_id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id integer NOT NULL REFERENCES books(book_id),
    member_id integer NOT NULL REFERENCES members(member_id),
    issued_date date NOT NULL DEFAULT CURRENT_DATE,
    due_date date NOT NULL CHECK (due_date >= issued_date),
    returned_date date CHECK (returned_date >= issued_date)
);
CREATE INDEX IF NOT EXISTS loans_active_member_idx ON loans(member_id) WHERE returned_date IS NULL;
CREATE INDEX IF NOT EXISTS loans_book_idx ON loans(book_id);
CREATE INDEX IF NOT EXISTS loans_member_idx ON loans(member_id);
CREATE INDEX IF NOT EXISTS loans_overdue_idx ON loans(due_date) WHERE returned_date IS NULL;
