create table loan_application
(
    id          uuid           primary key,
    first_name  varchar(100)   not null,
    last_name   varchar(100)   not null,
    email       varchar(320)   not null,
    amount      numeric(14, 2) not null,
    loan_terms  integer        not null,
    status      varchar(20)    not null,
    created_at  timestamptz    not null
);

create table lender_offer
(
    id                     uuid           primary key,
    application_id         uuid           not null references loan_application (id),
    lender_name            varchar(100)   not null,
    annual_interest_rate   numeric(5, 2)  not null,
    monthly_payment_amount numeric(14, 2) not null,
    total_repayment        numeric(14, 2) not null,
    status                 varchar(20)    not null,
    created_at             timestamptz    not null
);

create unique index ux_lender_offer_application_lender
    on lender_offer (application_id, lender_name);

create index ix_lender_offer_application_status
    on lender_offer (application_id, status);

create index ix_loan_application_status_created_at
    on loan_application (status, created_at);
