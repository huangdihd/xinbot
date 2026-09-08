#!/usr/bin/env python3
"""Xinbot 遥测接收端(UDP) — 监听并解密打印遥测心跳/崩溃报告

用法:
  python3 scripts/telemetry-grabber.py                # 监听 127.0.0.1:9000
  python3 scripts/telemetry-grabber.py 0.0.0.0 9000   # 自定义监听地址/端口

依赖: pip install cryptography

与 config.conf 中 telemetry 块对应:
  telemetry.enable=true  telemetry.mode="udp"
  telemetry.ip=<本机地址>  telemetry.port=<端口>

报文格式(Java 端 TelemetryManager):
  [0..3]   magic "XBTL"
  [4]      protocol version (1)
  [5]      message type (1=heartbeat, 2=crash)
  [6..17]  AES-GCM 12-byte IV
  [18..]   AES-128-GCM 密文(JSON + 16 字节认证标签)
固定密钥(收发双方一致): b"xinbot-telemetry"
"""

import argparse
import json
import socket
import sys

try:
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
except ImportError:
    print("[!] 缺少 cryptography 依赖,请先执行: pip install cryptography")
    sys.exit(1)

MAGIC = b"XBTL"
KEY = b"xinbot-telemetry"  # 16 字节 AES-128,与遥测端固定密钥一致
HEADER_LEN = 18  # 4 magic + 1 version + 1 type + 12 iv
TYPE_NAMES = {1: "heartbeat", 2: "crash"}


def decrypt(data):
    """校验信封并返回解密后的 JSON 文本"""
    if len(data) < HEADER_LEN:
        raise ValueError(f"数据包过短: {len(data)} 字节")
    if data[:4] != MAGIC:
        raise ValueError(f"魔数不匹配: {data[:4]!r}")
    version = data[4]
    if version != 1:
        raise ValueError(f"不支持的协议版本: {version}")
    msg_type = data[5]
    iv = data[6:HEADER_LEN]
    ciphertext = data[HEADER_LEN:]
    plaintext = AESGCM(KEY).decrypt(iv, ciphertext, None)
    return TYPE_NAMES.get(msg_type, f"type-{msg_type}"), plaintext.decode("utf-8")


def listen(host, port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((host, port))
    print(f"[*] 正在监听 UDP {host}:{port},等待 Xinbot 遥测数据...(Ctrl+C 退出)\n")
    while True:
        data, addr = sock.recvfrom(65535)
        try:
            kind, text = decrypt(data)
        except Exception as e:
            print(f"[!] 来自 {addr[0]}:{addr[1]} 的包解密失败: {e}\n")
            continue
        print(f"{'=' * 60}")
        print(f"[{kind}] 来自 {addr[0]}:{addr[1]},长度 {len(data)} 字节")
        try:
            obj = json.loads(text)
            print(json.dumps(obj, ensure_ascii=False, indent=2))
        except json.JSONDecodeError:
            print(text)
        print("=" * 60 + "\n")


def main():
    parser = argparse.ArgumentParser(description="Xinbot 遥测 UDP 接收端")
    parser.add_argument("host", nargs="?", default="127.0.0.1", help="监听地址(默认 127.0.0.1)")
    parser.add_argument("port", nargs="?", type=int, default=9000, help="监听端口(默认 9000)")
    args = parser.parse_args()
    try:
        listen(args.host, args.port)
    except KeyboardInterrupt:
        print("\n[!] 已退出")


if __name__ == "__main__":
    main()
