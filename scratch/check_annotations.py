import urllib.request
import json

url = "https://api.github.com/repos/Yam12221/autoclik-pro-max/actions/runs/30825910980/annotations"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))
        print(json.dumps(data, indent=2))
except Exception as e:
    print(f"Error: {e}")
