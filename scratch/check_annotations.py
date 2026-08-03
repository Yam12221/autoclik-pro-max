import urllib.request
import json
import sys

run_id = sys.argv[1] if len(sys.argv) > 1 else "30829468070"
url = f"https://api.github.com/repos/Yam12221/autoclik-pro-max/actions/runs/{run_id}/annotations"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))
        for item in data:
            print(f"Annotation: {item.get('title')} | Message: {item.get('message')}")
except Exception as e:
    print(f"Error: {e}")
