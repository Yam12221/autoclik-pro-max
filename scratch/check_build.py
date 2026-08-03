import urllib.request
import json
import time

url = "https://api.github.com/repos/Yam12221/autoclik-pro-max/actions/runs"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))
        runs = data.get('workflow_runs', [])
        if runs:
            latest = runs[0]
            print(f"Workflow: {latest.get('name')}")
            print(f"Status: {latest.get('status')}")
            print(f"Conclusion: {latest.get('conclusion')}")
            print(f"HTML URL: {latest.get('html_url')}")
        else:
            print("No runs found")
except Exception as e:
    print(f"Error: {e}")
