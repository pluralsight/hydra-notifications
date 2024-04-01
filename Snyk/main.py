import json
import os

def readsynk_json():
    try:
        file=open("snyk-code-analysis.json","r")
        content=file.read()
        file = open(".\Snyk\slack_template.json", "r")
        msg_template = file.read()
        file.close()
        return json.loads(content),msg_template
    except Exception as e:
            print("Exception Occurred"+str(e))

def cook_slack_meesage(webhook_url):
    try:
        vulnerability=[]
        commit_sha=os.getenv("CI_COMMIT_SHORT_SHA", default=None)
        service_name=os.getenv("SERVICE_NAME",default=None)
        high,med,critical,low = 0,0,0,0
        content,msg_template=readsynk_json()
        for vul in content['vulnerabilities']:
            vulnerability.append("{}:{}".format(vul['id'],vul['severity']))
        vulnerability=(list(set(vulnerability)))
        for vul in vulnerability:
            sev=vul.split(':')[1]
            if sev.lower() == "high":
                high=high+1
            elif sev.lower() == "medium":
                med=med+1
            elif sev.lower() == "low":
                low=low+1
            elif sev.lower() == "crtitcal":
                critical=critical+1
        msg_template=msg_template.replace("<LOW>",str(low))
        msg_template=msg_template.replace("<MEDIUM>", str(med))
        msg_template=msg_template.replace("<HIGH>", str(high))
        msg_template=msg_template.replace("<CRITICAL>", str(critical))
        msg_template = msg_template.replace("<TOTAL_VUL>", str(len(vulnerability)))
        msg_template = msg_template.replace("<ServiceName>", str(service_name))
        msg_template = msg_template.replace("<CommitID>", str(len(commit_sha)))
        return msg
    except Exception as e:
        print("Exception Occurred" + str(e))

msg=cook_slack_meesage(webhook_url)
print(msg)