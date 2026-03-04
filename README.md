# search-service

책 검색·자동완성·인기검색어·검색 로그를 제공하는 MSA 검색 서비스입니다.

## 요구 사항

- Java 17
- Elasticsearch (로컬: `http://localhost:9200`)
- Kakao 도서 검색 API 키 (`kakao.api.key` 또는 `KAKAO_API_KEY`)

### Elasticsearch 실행 (로컬, 단독 실행 시)

Elasticsearch를 이 프로젝트에서 띄우지 않고 **geonpil Docker의 Elasticsearch**를 쓰는 경우에는 아래 Docker 실행만 하면 됩니다.

## Docker 실행 (geonpil-network)

geonpil 쪽에 `geonpil-net`와 Elasticsearch가 이미 떠 있을 때, search-service만 같은 네트워크에 붙여 실행합니다.

### 1. `.env` 파일 준비 (필수)

인증 정보는 `docker-compose.yml`에 넣지 않고 `.env`에서 읽습니다. (`.env`는 Git에 올라가지 않습니다.)

```bash
# .env.example을 복사한 뒤 값을 채움
copy .env.example .env
# .env를 열어 ELASTICSEARCH_PASSWORD 등 수정
```

- **필수**: `ELASTICSEARCH_USERNAME`, `ELASTICSEARCH_PASSWORD` (ES 컨테이너의 `ELASTIC_PASSWORD`와 동일하게)
- 선택: `ELASTICSEARCH_HOST`, `ELASTICSEARCH_SSL_INSECURE`, `KAKAO_API_KEY`

### 2. 컨테이너 실행

```bash
cd c:\geonpil2\search-service
docker-compose up -d --build
```

- **전제**: geonpil 프로젝트에서 `geonpil-net`이 생성되어 있고, 그 안에 Elasticsearch 컨테이너가 기동 중이어야 합니다.
- Elasticsearch 서비스 이름이 `elasticsearch`가 아니면 `.env`의 `ELASTICSEARCH_HOST`를 해당 이름으로 수정 (예: `https://es:9200`).

중지: `docker-compose down`

## 설정

- `src/main/resources/application-dev.yml`
  - `elasticsearch.host`, `elasticsearch.username`, `elasticsearch.password`
  - `kakao.api.key` (또는 환경변수 `KAKAO_API_KEY`)
- 기본 포트: **8081** (geonpil 메인 앱은 8080)

## 빌드 및 실행

Gradle Wrapper가 없으면 **geonpil** 프로젝트 루트에서:

```bash
cd c:\geonpil2\geonpil
.\gradlew.bat -p ..\search-service bootRun
```

또는 search-service에 Gradle Wrapper를 생성한 뒤:

```bash
cd c:\geonpil2\search-service
.\gradlew.bat bootRun
```

## API (기본 base: `http://localhost:8081`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/search/books?query=...&page=1&size=15` | 책 검색 |
| GET | `/api/search/suggestions?q=...&limit=10` | 자동완성 |
| GET | `/api/search/popular?topN=10` | 인기 검색어 |
| POST | `/api/search/log` | 검색 로그 저장 (body: `keyword`, `userId`, `ip`, `userAgent`) |

## geonpil과 연동

geonpil 쪽에서 `BookSearchFacade`의 **원격 구현체**(예: `RestBookSearchFacade`)를 만들어  
위 API를 호출하도록 하면, 기존 화면/컨트롤러 수정 없이 검색 기능을 search-service로 이전할 수 있습니다.
