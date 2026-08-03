import urllib.request
import json
import sys

run_id = sys.argv[1] if len(sys.argv) > 1 else "30829247592"
url = f"https://api.github.com/repos/Yam12221/autoclik-pro-max/actions/runs/{run_id}/jobs"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))
        job_id = data['jobs'][0]['id']
        print(f"Job ID: {job_id}")

        log_url = f"https://api.github.com/repos/Yam12221/autoclik-pro-max/actions/jobs/{job_id}/logs"
        log_req = urllib.request.Request(log_url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(log_req) as log_resp:
            log_content = log_resp.read().decode('utf-8', errors='ignore')
            lines = log_content.splitlines()
            print("--- LOG TAIL ---")
            for l in lines[-100:]:
                print(l)
except Exception as e:
    print(f"Error: {e}")
