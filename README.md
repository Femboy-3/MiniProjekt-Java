### Funkcija: `login_user`

Funkcija za prijavo uporabnika, ki preveri geslo z uporabo `crypt()` iz pgcrypto.  
Če je geslo pravilno, vrne ID uporabnika in ali je admin, drugače vrne 0 in false.

```sql
CREATE OR REPLACE FUNCTION login_user(emaili VARCHAR, passwordi VARCHAR)
RETURNS TABLE(userid INTEGER, isadmin BOOLEAN) AS $$
DECLARE
    stored_password VARCHAR;
    uid INTEGER;
    admin_flag BOOLEAN;
BEGIN
    SELECT id, password, admin INTO uid, stored_password, admin_flag
    FROM Users
    WHERE email = emaili;

    IF stored_password IS NOT NULL AND stored_password = crypt(passwordi, stored_password) THEN
        userid := uid;
        isadmin := admin_flag;
    ELSE
        userid := 0;
        isadmin := false;
    END IF;

    RETURN NEXT;
END;
$$ LANGUAGE plpgsql;
```

Funkcija: register_user

Funkcija za registracijo novega uporabnika. Geslo se shrani šifrirano z crypt() in soljo bf.
Če uporabnik že obstaja (unikatna kršitev), vrne FALSE.

```sql
CREATE OR REPLACE FUNCTION register_user(usernamei VARCHAR, emaili VARCHAR, passwordi VARCHAR, phone_numi VARCHAR)
RETURNS BOOLEAN AS $$
BEGIN
    INSERT INTO Users (username, email, password, phone_num)
    VALUES (usernamei, emaili, crypt(passwordi, gen_salt('bf')), phone_numi);

    RETURN TRUE;
EXCEPTION
    WHEN unique_violation THEN
        RETURN FALSE;
END;
$$ LANGUAGE plpgsql;
```

Funkcija: get_all_routes

Vrne vse poti z informacijami o začetni in končni lokaciji, številu točk zanimanja, itd.

```sql
CREATE OR REPLACE FUNCTION get_all_routes()
RETURNS TABLE (
    id INT,
    name varchar,
    length FLOAT,
    difficulty INT,
    duration FLOAT,
    description TEXT,
    num_of_poi INT,
    start_location_name varchar,
    end_location_name varchar,
    date_created TIMESTAMP
)
AS $$
BEGIN
    RETURN QUERY
    SELECT
        r.id,
        r.name,
        r.length,
        r.difficulty,
        r.duration,
        r.description,
        r.num_of_poi,
        sl.name AS start_location_name,
        el.name AS end_location_name,
        r.date_created
    FROM routs r
    JOIN citys sl ON r.startlocation_id = sl.id
    JOIN citys el ON r.endlocation_id = el.id;
END;
$$ LANGUAGE plpgsql;
```

Funkcija: delete_route

Izbriše pot in vse povezane vnose iz tabele route_poi in review_user.

```sql
CREATE OR REPLACE FUNCTION delete_route(route_idi INT)
RETURNS void AS $$
BEGIN
    DELETE FROM route_poi WHERE route_id = route_idi;

    DELETE FROM review_user WHERE route_id = route_idi;

    DELETE FROM routs WHERE id = route_idi;
END;
$$ LANGUAGE plpgsql;
```

Funkcija: get_comments_with_usernames

Vrne komentarje za določeno pot skupaj z uporabniškimi imeni.

```sql
CREATE OR REPLACE FUNCTION get_comments_with_usernames(route_idi INT)
RETURNS TABLE(comment_id INT, username VARCHAR, comment_text TEXT, user_id INT) AS $$
BEGIN
    RETURN QUERY
    SELECT c.id, u.username, c.description, c.user_id
    FROM review_user c
    JOIN users u ON c.user_id = u.id
    WHERE c.route_id = route_idi
    ORDER BY c.id;
END;
$$ LANGUAGE plpgsql;
```

Funkcija: delete_comment

Izbriše komentar, če je uporabnik admin ali lastnik komentarja.
Vrne TRUE če je brisanje uspešno, drugače FALSE.

```sql
CREATE OR REPLACE FUNCTION delete_comment(comment_idi INT, current_user_idi INT, is_admini BOOLEAN)
RETURNS BOOLEAN AS $$
DECLARE
    comment_owner INT;
BEGIN
    SELECT user_id INTO comment_owner FROM review_user WHERE id = comment_idi;

    IF comment_owner IS NULL THEN
        RETURN FALSE;
    END IF;

    IF is_admini OR comment_owner = current_user_idi THEN
        DELETE FROM review_user WHERE id = comment_idi;
        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END;
$$ LANGUAGE plpgsql;
```

Funkcija: add_comment

Doda komentar uporabnika (ali anonimnega, če je userid = -1).

```sql
CREATE OR REPLACE FUNCTION add_comment(routid INT, userid INT, commenttext TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    INSERT INTO review_user(route_id, user_id, description)
    VALUES (routid, userid, commenttext);
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;
```

Funkcija: log_deleted_comment in before_delete_comment

Trigger in funkcija, ki zabeleži izbrisane komentarje v log tabelo.

```sql
CREATE OR REPLACE FUNCTION log_deleted_comment()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO review_user_log(route_id, user_id, description)
    VALUES (OLD.route_id, OLD.user_id, OLD.description);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER before_delete_comment
BEFORE DELETE ON review_user
FOR EACH ROW
EXECUTE FUNCTION log_deleted_comment();
```

Funkcija: insert_route

Vstavi novo pot z začetno in končno lokacijo ter povezavo s točkami zanimanja (pois).

```sql
CREATE OR REPLACE FUNCTION insert_route(
    p_name VARCHAR,
    p_length FLOAT,
    p_difficulty INT,
    p_duration FLOAT,
    p_description TEXT,
    p_start_location_name VARCHAR,
    p_end_location_name VARCHAR,
    p_pois INT[]
) RETURNS INT AS $$
DECLARE
    start_location_id INT;
    end_location_id INT;
    rout_id INT;
    p_id INT;
BEGIN
    SELECT id INTO start_location_id FROM citys WHERE name = p_start_location_name LIMIT 1;
    SELECT id INTO end_location_id FROM citys WHERE name = p_end_location_name LIMIT 1;

    INSERT INTO routs(name, length, difficulty, duration, description, startlocation_id, endlocation_id)
    VALUES (p_name, p_length, p_difficulty, p_duration, p_description, start_location_id, end_location_id)
    RETURNING id INTO rout_id;

    FOREACH p_id IN ARRAY p_pois LOOP
        INSERT INTO route_poi(route_id, poi_id) VALUES (rout_id, p_id);
    END LOOP;

    RETURN rout_id;
END;
$$ LANGUAGE plpgsql;
```

Trigger: update_num_of_poi_trigger in update_num_of_poi

Trigger za posodobitev števila POI po vstavljanju v route_poi.

```sql
CREATE TRIGGER update_num_of_poi_trigger
AFTER INSERT ON route_poi
FOR EACH ROW
EXECUTE FUNCTION update_num_of_poi();

CREATE OR REPLACE FUNCTION update_num_of_poi()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE routs
    SET num_of_poi = (SELECT COUNT(*) FROM route_poi WHERE route_id = NEW.route_id)
    WHERE id = NEW.route_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

Trigger: update_num_of_poi_after_delete_trigger in update_num_of_poi_after_delete

```sql
CREATE OR REPLACE FUNCTION update_num_of_poi_after_delete()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE routs
    SET num_of_poi = (SELECT COUNT(*) FROM route_poi WHERE route_id = OLD.route_id)
    WHERE id = OLD.route_id;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_num_of_poi_after_delete_trigger
AFTER DELETE ON route_poi
FOR EACH ROW
EXECUTE FUNCTION update_num_of_poi_after_delete();
```

Funkcija: get_route_by_id

Vrne podrobnosti poti glede na ID.

```sql
CREATE OR REPLACE FUNCTION get_route_by_id(route_id INTEGER)
RETURNS TABLE (
    id INTEGER,
    name VARCHAR(255),
    length FLOAT,
    difficulty INTEGER,
    duration FLOAT,
    description TEXT,
    start_location_name VARCHAR(255),
    end_location_name VARCHAR(255)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        r.id,
        r.name,
        r.length,
        r.difficulty,
        r.duration,
        r.description,
        cs.name AS start_location_name,
        ce.name AS end_location_name
    FROM routs r
    JOIN citys cs ON cs.id = r.startlocation_id
    JOIN citys ce ON ce.id = r.endlocation_id
    WHERE r.id = route_id;
END;
$$ LANGUAGE plpgsql;
```

Funkcija: get_route_pois

Vrne ID-je točk zanimanja povezane s potjo.

```sql
CREATE OR REPLACE FUNCTION get_route_pois(route_idi INTEGER)
RETURNS TABLE (
    poi_ids INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT poi_id
    FROM route_poi
    WHERE route_id = route_idi;
END;
$$ LANGUAGE plpgsql;
```

Funkcija: update_route

Posodobi podatke o poti in njene povezave s točkami zanimanja.

```sql
CREATE OR REPLACE FUNCTION update_route(
    p_id INTEGER,
    p_name TEXT,
    p_length FLOAT,
    p_difficulty INTEGER,
    p_duration FLOAT,
    p_description TEXT,
    p_start_city TEXT,
    p_end_city TEXT,
    p_poi_ids INTEGER[]
) RETURNS VOID AS $$
DECLARE
    start_id INTEGER;
    end_id INTEGER;
BEGIN
    SELECT id INTO start_id FROM citys WHERE name = p_start_city;
    SELECT id INTO end_id FROM citys WHERE name = p_end_city;

    UPDATE routs
    SET name = p_name,
        length = p_length,
        difficulty = p_difficulty,
        duration = p_duration,
        description = p_description,
        startlocation_id = start_id,
        endlocation_id = end_id
    WHERE id = p_id;

    DELETE FROM route_poi WHERE route_id = p_id;

    INSERT INTO route_poi (route_id, poi_id)
    SELECT p_id, unnest(p_poi_ids);
END;
$$ LANGUAGE plpgsql;
```
