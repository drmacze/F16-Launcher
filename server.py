import http.server
import socketserver

PORT = 5000
HOST = "0.0.0.0"

class Handler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-type", "text/html; charset=utf-8")
        self.end_headers()
        with open("index.html", "rb") as f:
            self.wfile.write(f.read())

    def log_message(self, format, *args):
        pass

with socketserver.TCPServer((HOST, PORT), Handler) as httpd:
    print(f"DLavie 26 dashboard running on {HOST}:{PORT}")
    httpd.serve_forever()
