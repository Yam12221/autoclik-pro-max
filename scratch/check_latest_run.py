import urllib.request
import json

url = "https://api.github.com/repos/Yam12221/autoclik-pro-max/actions/runs"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))
        runs = data.get('workflow_runs', [])
        if runs:
            latest = runs[0]
            print(f"Run ID: {latest.get('id')}")
            print(f"Workflow: {latest.get('name')}")
            print(f"Status: {latest.get('status')}")
            print(f"Conclusion: {latest.get('conclusion')}")

            jobs_url = latest.get('jobs_url')
            jobs_req = urllib.request.Request(jobs_url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(jobs_req) as jresp:
                jdata = json.loads(jresp.read().decode('utf-8'))
                for job in jdata.get('jobs', []):
                    print(f"Job: {job.get('name')} | Status: {job.get('status')} | Conclusion: {job.get('conclusion')}")
                    for step in job.get('steps', []):
                        print(f"   Step: {step.get('name')} | Status: {step.get('status')} | Conclusion: {step.get('conclusion')}")
except Exception as e:
    print(f"Error: {e}")
