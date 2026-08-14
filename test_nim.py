import requests

url = "https://integrate.api.nvidia.com/v1/chat/completions"
headers = {
    "Authorization": "Bearer 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",
    "Content-Type": "application/json"
}
data = {
    "model": "meta/llama-3.1-8b-instruct",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 50
}

try:
    response = requests.post(url, headers=headers, json=data, timeout=10)
    print("Status Code:", response.status_code)
    print("Response:", response.text)
except Exception as e:
    print("Error:", e)
