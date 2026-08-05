#!/usr/bin/env python3
"""QQ Bot WebSocket 事件监听器 — 连接后打印所有事件，用于获取 group_openid

用法:
  python3 scripts/qq-event-grabber.py <APP_ID> <APP_SECRET>
  或
  QQ_BOT_APP_ID=xxx QQ_BOT_APP_SECRET=xxx python3 scripts/qq-event-grabber.py
"""

import asyncio
import json
import os
import sys
import urllib.request
import ssl

# ========== 1. 获取 Access Token ==========

def get_access_token(app_id, app_secret):
    """用 AppID + AppSecret 获取 access_token"""
    url = "https://bots.qq.com/app/getAppAccessToken"
    req = urllib.request.Request(
        url,
        data=json.dumps({"appId": app_id, "clientSecret": app_secret}).encode(),
        headers={"Content-Type": "application/json"},
    )
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read())
    token = data.get("access_token")
    if not token:
        print(f"[!] 获取 token 失败: {data}")
        sys.exit(1)
    print(f"[✓] access_token: {token[:20]}...")
    return token

# ========== 2. 获取 WebSocket 网关地址 ==========

def get_gateway_url(token):
    """获取 WSS 连接地址"""
    req = urllib.request.Request(
        "https://api.bot.qq.com/gateway/bot",
        headers={"Authorization": f"QQBot {token}"},
    )
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read())
    url = data.get("url")
    if not url:
        print(f"[!] 获取网关失败: {data}")
        sys.exit(1)
    print(f"[✓] WSS URL: {url[:50]}...")
    return url

# ========== 3. WebSocket 事件监听 ==========

async def listen_events(wss_url, token):
    import aiohttp

    HEARTBEAT_SEC = 30

    async with aiohttp.ClientSession() as session:
        async with session.ws_connect(wss_url) as ws:
            seq = 0
            session_id = None

            async def heartbeat():
                nonlocal seq
                while True:
                    await asyncio.sleep(HEARTBEAT_SEC)
                    try:
                        await ws.send_json({"op": 1, "d": seq} if seq else {"op": 1})
                    except Exception:
                        break

            async def reader():
                nonlocal seq, session_id
                async for msg in ws:
                    if msg.type == aiohttp.WSMsgType.TEXT:
                        data = json.loads(msg.data)
                        op = data.get("op", -1)
                        event_type = data.get("t", "")
                        d = data.get("d", {})

                        if op == 10:  # Hello
                            print(f"[✓] Hello, heartbeat interval: {d.get('heartbeat_interval', HEARTBEAT_SEC)}ms")
                            # 发送鉴权
                            payload = {
                                "op": 2,
                                "d": {
                                    "token": f"QQBot {token}",
                                    "intents": (1 << 25),  # GROUP_AND_C2C_EVENT
                                    "shard": [0, 1],
                                },
                            }
                            await ws.send_json(payload)
                            print("[→] 已发送鉴权请求")

                        elif op == 11:  # Heartbeat ACK
                            pass

                        elif op == 0:  # Dispatch - 事件！
                            seq = data.get("s", seq)
                            print(f"\n{'='*60}")
                            print(f"[事件] op={op} type={event_type}")
                            print(json.dumps(d, ensure_ascii=False, indent=2))

                            # 提取 group_openid
                            if isinstance(d, dict):
                                if "group_openid" in d:
                                    print(f"\n  ★★★ group_openid = {d['group_openid']} ★★★")
                                # 也可能是嵌套在 data 里
                                if "d" in d and isinstance(d["d"], dict) and "group_openid" in d["d"]:
                                    print(f"\n  ★★★ group_openid = {d['d']['group_openid']} ★★★")
                            print(f"{'='*60}")

                        elif op == 9:  # Invalid Session
                            print(f"[!] Invalid session: {d}")
                            break

            print("[*] 开始监听事件... 把机器人拉进群或在群里 @机器人 试试")
            print("[*] Ctrl+C 退出\n")

            hb_task = asyncio.create_task(heartbeat())
            try:
                await reader()
            finally:
                hb_task.cancel()

# ========== 主入口 ==========

async def main():
    app_id = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("QQ_BOT_APP_ID")
    app_secret = sys.argv[2] if len(sys.argv) > 2 else os.environ.get("QQ_BOT_APP_SECRET")

    if not app_id or not app_secret:
        print("用法: python3 scripts/qq-event-grabber.py <APP_ID> <APP_SECRET>")
        print("  或: QQ_BOT_APP_ID=xxx QQ_BOT_APP_SECRET=xxx python3 scripts/qq-event-grabber.py")
        sys.exit(1)

    token = get_access_token(app_id, app_secret)
    wss_url = get_gateway_url(token)
    await listen_events(wss_url, token)

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n[!] 已退出")
