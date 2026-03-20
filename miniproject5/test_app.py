from app import app

def test_hello():
    client = app.test_client()
    response = client.get('/hello')
    assert response.status_code == 200
    assert b"Hello World" in response.data

def test_add():
    client = app.test_client()
    response = client.post('/add', data={"name": "Tung"})
    assert response.status_code == 200
    assert b"Tung" in response.data

def test_list():
    client = app.test_client()
    client.post('/add', data={"name": "Tung"})
    response = client.get('/list')
    assert response.status_code == 200
    assert b"Tung" in response.data


