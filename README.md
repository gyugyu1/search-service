# search-service

책 검색 기능을 **독립 서비스(MSA)** 로 분리한 검색 백엔드입니다.  
`geonpil` 본 서비스(또는 다른 클라이언트)가 HTTP로 호출해 **도서 검색 / 자동완성 / 인기검색어 / 검색 로그 수집**을 제공합니다.

## 핵심 기능

- **도서 검색 API**: Kakao 도서 검색 API를 호출하고, 응답을 서비스 화면에 맞는 DTO로 가공해 반환
- **검색 로그 수집**: 검색어/사용자/클라이언트 정보를 Elasticsearch에 저장
- **인기검색어**: Elasticsearch terms aggregation으로 Top N 키워드 집계
- **자동완성**: 입력 prefix 기반으로 검색 결과의 title을 후보로 만들고 중복 제거/limit 처리

## 기술 스택

- **Java 17**
- **Spring Boot 3.1.x**
  - `spring-boot-starter-web` (REST API)
  - `spring-boot-starter-data-elasticsearch` + Elasticsearch Java API Client 사용
- **Elasticsearch**
  - 검색 로그 인덱스(`book_search_log`) 자동 생성/매핑 보정
- **Docker**
  - 멀티 스테이지 빌드로 `bootJar` 생성 후 JRE 이미지로 실행
- **GitHub Actions**
  - main push 시 Docker 이미지 빌드/푸시 → self-hosted runner에서 compose pull & up

## 아키텍처 / 데이터 흐름

- **검색**
  - Client → `GET /api/search/books` → `BookSearchService` → Kakao API 호출 → 응답 DTO 변환 → Client
- **로그 수집**
  - Client → `POST /api/search/log` → `BookSearchLogService` → Elasticsearch index
- **인기검색어**
  - Client → `GET /api/search/popular` → Elasticsearch aggregation(terms) → Top N 반환
- **자동완성**
  - Client → `GET /api/search/suggestions` → (prefix로 1차 검색) → title 후보 추출/정제

## API

Base URL: `http://localhost:8081`

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/search/books?query=...&page=1&size=15` | 책 검색 |
| GET | `/api/search/suggestions?q=...&limit=10` | 자동완성 |
| GET | `/api/search/popular?topN=10` | 인기 검색어 |
| POST | `/api/search/log` | 검색 로그 저장 |

`POST /api/search/log` body 예시:

```json
{
  "keyword": "클린 코드",
  "userId": 1,
  "ip": "127.0.0.1",
  "userAgent": "Mozilla/5.0 ..."
}
```

## 설정 (환경변수)

민감정보는 커밋하지 않고 **환경변수/.env**로 주입합니다.

- **필수**
  - `KAKAO_API_KEY`
- **Elasticsearch**
  - `ELASTICSEARCH_HOST` (예: `https://elasticsearch:9200`)
  - `ELASTICSEARCH_USERNAME`, `ELASTICSEARCH_PASSWORD`
  - dev: `ELASTICSEARCH_SSL_INSECURE=true` (자체 서명 인증서 허용)
  - prod(선택): `ELASTICSEARCH_SSL_TRUSTSTORE_PATH`, `ELASTICSEARCH_SSL_TRUSTSTORE_PASSWORD`
- **서버**
  - `SERVER_PORT` (기본 8081)

예시 파일은 `.env.example`를 참고하세요.

## 로컬 실행

### 1) 애플리케이션만 실행 (Gradle)

```bash
cd c:\geonpil2\search-service
.\gradlew.bat bootRun
```

### 2) Docker로 실행 (외부 Elasticsearch 사용)

전제: 같은 네트워크(`geonpil-net`)에 Elasticsearch가 실행 중이어야 합니다.

```bash
cd c:\geonpil2\search-service
copy .env.example .env
docker-compose up -d --build
```

- 종료: `docker-compose down`

## 배포

`.github/workflows/deploy.yml`

- main push 시 DockerHub에 이미지 push
  - `${DOCKERHUB_USERNAME}/search-service:latest`
  - `${DOCKERHUB_USERNAME}/search-service:${GITHUB_SHA}`
- 이후 self-hosted runner(`prod` 라벨)에서
  - 운영 서버의 compose(`docker-compose.prod.yml`)로 pull & up -d 수행

## 프로젝트 구조 (요약)

- `src/main/java/com/geonpil/searchservice/controller` : REST API 컨트롤러
- `src/main/java/com/geonpil/service` : 검색/로그 서비스 로직
- `src/main/java/com/geonpil/config` : Jackson/Elasticsearch 클라이언트 설정
- `src/main/resources` : `application.yml`, `application-dev.yml`, `application-prod.yml`

## geonpil과의 연동 의도

본 서비스는 검색 기능을 “외부 REST API”로 분리해, `geonpil` 본 서비스에서는 facade/adapter 형태로 호출하도록 설계했습니다.  
즉, 화면/유스케이스는 유지하면서 검색 구현을 독립적으로 배포·확장할 수 있습니다.
