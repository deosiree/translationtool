"""从环境变量 / .env 加载应用配置。

运行时唯一真相源为项目根目录下的 `.env` 文件。
`.env.example` 仅为首次 setup 的复制模板，代码不会读取它。
缺失 `.env` 或必填项时，Settings 初始化将报错。
"""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Agent 运行时配置项 — 字段值均从 `.env` 读取，此处不设 Python 默认值。"""

    # 服务监听（对应 .env：AGENT_HOST / AGENT_PORT）
    agent_host: str
    agent_port: int

    # MySQL 连接（对应 .env：MYSQL_*）
    mysql_host: str
    mysql_port: int
    mysql_user: str
    mysql_password: str
    mysql_database: str

    # 大模型（DeepSeek / OpenAI 兼容接口；对应 .env：LLM_*）
    llm_api_key: str
    llm_base_url: str
    llm_model: str
    llm_temperature: float

    # LangSmith（可选；未配 LANGSMITH_API_KEY 时不启用上报）
    langsmith_tracing: bool = False
    langsmith_api_key: str | None = None
    langsmith_project: str = "translationtool-agent"

    @property
    def database_url(self) -> str:
        """组装 SQLAlchemy 同步连接 URL（engine 侧再换 aiomysql）。"""
        return (
            f"mysql+pymysql://{self.mysql_user}:{self.mysql_password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_database}"
            "?charset=utf8mb4"
        )

    # 仅从 `.env` 加载；不在代码中维护业务默认值
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
    )


settings = Settings()
