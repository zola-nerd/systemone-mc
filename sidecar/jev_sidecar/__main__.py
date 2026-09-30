import uvicorn


def main() -> None:
    uvicorn.run("jev_sidecar.app:app", host="127.0.0.1", port=8765, log_level="info")


if __name__ == "__main__":
    main()
