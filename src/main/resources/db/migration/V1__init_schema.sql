-- API Atlas initial schema

create table discovery_session (
    id              bigserial primary key,
    name            varchar(255)  not null,
    target          varchar(2048) not null,
    type            varchar(20)   not null,
    status          varchar(20)   not null,
    browser         varchar(30),
    platform        varchar(30),
    pages_visited   int           not null default 0,
    endpoints_found int           not null default 0,
    error_message   text,
    started_at      timestamptz   not null,
    ended_at        timestamptz
);

create table api_category (
    id          bigserial primary key,
    name        varchar(120) not null unique,
    description text
);

create table api_endpoint (
    id                    bigserial primary key,
    session_id            bigint references discovery_session (id) on delete set null,
    category_id           bigint references api_category (id) on delete set null,
    name                  varchar(255)  not null,
    module                varchar(120),
    service               varchar(255)  not null,
    url                   varchar(2048) not null,
    path                  varchar(1000) not null,
    method                varchar(10)   not null,
    version               varchar(20),
    protocol              varchar(20)   not null,
    auth_type             varchar(20)   not null,
    status                varchar(20)   not null,
    third_party           boolean       not null default false,
    description           text,
    business_purpose      text,
    request_description   text,
    response_description  text,
    test_recommendations  text,
    sample_request        text,
    sample_response       text,
    hit_count             bigint        not null default 0,
    first_seen            timestamptz   not null,
    last_seen             timestamptz   not null,
    constraint uq_endpoint unique (method, service, path)
);
create index idx_endpoint_status on api_endpoint (status);
create index idx_endpoint_service on api_endpoint (service);
create index idx_endpoint_last_seen on api_endpoint (last_seen);

create table api_request (
    id           bigserial primary key,
    endpoint_id  bigint      not null references api_endpoint (id) on delete cascade,
    headers      text,
    query_params text,
    cookies      text,
    body         text,
    captured_at  timestamptz not null
);
create index idx_request_endpoint on api_request (endpoint_id, captured_at desc);

create table api_response (
    id               bigserial primary key,
    endpoint_id      bigint      not null references api_endpoint (id) on delete cascade,
    request_id       bigint references api_request (id) on delete set null,
    status_code      int         not null,
    headers          text,
    body             text,
    content_type     varchar(255),
    response_time_ms bigint      not null default 0,
    captured_at      timestamptz not null
);
create index idx_response_endpoint on api_response (endpoint_id, captured_at desc);

create table api_tag (
    id          bigserial primary key,
    endpoint_id bigint      not null references api_endpoint (id) on delete cascade,
    tag         varchar(80) not null,
    constraint uq_tag unique (endpoint_id, tag)
);

create table api_relationship (
    id                 bigserial primary key,
    source_endpoint_id bigint      not null references api_endpoint (id) on delete cascade,
    target_endpoint_id bigint      not null references api_endpoint (id) on delete cascade,
    type               varchar(40) not null,
    weight             bigint      not null default 1,
    constraint uq_relationship unique (source_endpoint_id, target_endpoint_id, type)
);

create table api_statistics (
    id              bigserial primary key,
    endpoint_id     bigint           not null unique references api_endpoint (id) on delete cascade,
    call_count      bigint           not null default 0,
    error_count     bigint           not null default 0,
    avg_response_ms double precision not null default 0,
    max_response_ms bigint           not null default 0,
    updated_at      timestamptz      not null
);

create table security_finding (
    id             bigserial primary key,
    endpoint_id    bigint        not null references api_endpoint (id) on delete cascade,
    endpoint_label varchar(1100) not null,
    category       varchar(60)   not null,
    owasp          varchar(60),
    severity       varchar(20)   not null,
    title          varchar(255)  not null,
    description    text,
    detected_at    timestamptz   not null,
    resolved       boolean       not null default false,
    constraint uq_finding unique (endpoint_id, category)
);
create index idx_finding_severity on security_finding (severity) where resolved = false;
