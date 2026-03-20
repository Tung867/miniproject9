from flask import Flask, request

app = Flask(__name__)
students = []

@app.route('/hello')
def hello():
    return "Hello World"

@app.route('/add', methods=['POST'])
def add():
    name = request.form.get("name")
    students.append(name)
    return name

@app.route('/list')
def list_students():
    return "<br>".join(students)

@app.route('/')
def home():
    return """
    <h1>Student Manager</h1>
    <form method="POST" action="/add">
        <input type="text" name="name">
        <button type="submit">Add</button>
    </form>
    <a href="/list">View List</a>
    """
if __name__ == "__main__":
    app.run(debug=True)