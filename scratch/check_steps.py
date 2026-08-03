import urllib.request
import json

url = "https://api.github.com/repos/Yam12221/autoclik-pro-max/actions/runs/30825910980/jobs"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))
        jobs = data.get('jobs', [])
        for job in jobs:
            print(f"Job: {job.get('name')} | Status: {job.get('status')} | Conclusion: {job.get('conclusion')}")
            for step in job.get('steps', []):
                print(f"   Step: {step.get('name')} | Status: {step.get('status')} | Conclusion: {step.get('conclusion')}")
except Exception as e:
    print(f"Error: {e}")
