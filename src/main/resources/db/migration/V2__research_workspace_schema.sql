-- V2: Autonomous Research Workspace Schema

-- Workspaces
CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_workspaces_owner_id ON workspaces(owner_id);

-- Workspace Members
CREATE TABLE workspace_members (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_workspace_member UNIQUE (workspace_id, user_id)
);
CREATE INDEX idx_workspace_members_user ON workspace_members(user_id);
CREATE INDEX idx_workspace_members_ws ON workspace_members(workspace_id);

-- Projects
CREATE TABLE projects (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    research_objectives TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_projects_workspace_id ON projects(workspace_id);
CREATE INDEX idx_projects_created_by ON projects(created_by);

-- Research Runs
CREATE TABLE research_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    topic TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    depth VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    academic_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    plan_json TEXT,
    error_message TEXT,
    tokens_used BIGINT NOT NULL DEFAULT 0,
    total_sources_found INT NOT NULL DEFAULT 0,
    total_claims_extracted INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_research_runs_project_id ON research_runs(project_id);
CREATE INDEX idx_research_runs_status ON research_runs(status);

-- Research Steps
CREATE TABLE research_steps (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES research_runs(id) ON DELETE CASCADE,
    step_name VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    step_order INT NOT NULL,
    input_data TEXT,
    output_data TEXT,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_research_steps_run_id ON research_steps(run_id);

-- Sources
CREATE TABLE sources (
    id UUID PRIMARY KEY,
    url TEXT NOT NULL UNIQUE,
    title TEXT,
    domain VARCHAR(255),
    source_type VARCHAR(30) NOT NULL DEFAULT 'WEB',
    authors TEXT,
    published_date VARCHAR(50),
    snippet TEXT,
    raw_content TEXT,
    cleaned_content TEXT,
    content_hash VARCHAR(64),
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_sources_domain ON sources(domain);
CREATE INDEX idx_sources_url ON sources(url);

-- Run Sources (Join table with relevance)
CREATE TABLE run_sources (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES research_runs(id) ON DELETE CASCADE,
    source_id UUID NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    relevance_score DOUBLE PRECISION,
    relevance_explanation TEXT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_run_source UNIQUE (run_id, source_id)
);
CREATE INDEX idx_run_sources_run_id ON run_sources(run_id);
CREATE INDEX idx_run_sources_source_id ON run_sources(source_id);

-- Documents
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_id UUID REFERENCES sources(id) ON DELETE SET NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500),
    extracted_text TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_documents_project_id ON documents(project_id);

-- Document Chunks
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    source_id UUID REFERENCES sources(id) ON DELETE CASCADE,
    run_id UUID REFERENCES research_runs(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    token_count INT NOT NULL DEFAULT 0,
    start_char_offset INT,
    end_char_offset INT,
    embedding TEXT,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_document_chunks_run_id ON document_chunks(run_id);
CREATE INDEX idx_document_chunks_source_id ON document_chunks(source_id);

-- Claims
CREATE TABLE claims (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES research_runs(id) ON DELETE CASCADE,
    claim_text TEXT NOT NULL,
    category VARCHAR(100),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED',
    verification_explanation TEXT,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_claims_run_id ON claims(run_id);
CREATE INDEX idx_claims_status ON claims(verification_status);

-- Claim Evidence
CREATE TABLE claim_evidence (
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    source_id UUID NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    chunk_id UUID REFERENCES document_chunks(id) ON DELETE SET NULL,
    evidence_text TEXT NOT NULL,
    evidence_type VARCHAR(30) NOT NULL DEFAULT 'SUPPORTING',
    section_or_page VARCHAR(100),
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_claim_evidence_claim_id ON claim_evidence(claim_id);
CREATE INDEX idx_claim_evidence_source_id ON claim_evidence(source_id);

-- Reports
CREATE TABLE reports (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL UNIQUE REFERENCES research_runs(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    executive_summary TEXT,
    objectives_scope TEXT,
    methodology TEXT,
    key_findings TEXT,
    detailed_analysis TEXT,
    contradictions_limitations TEXT,
    conclusions TEXT,
    citations_formatted TEXT,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_reports_project_id ON reports(project_id);
CREATE INDEX idx_reports_run_id ON reports(run_id);

-- Citations
CREATE TABLE citations (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    source_id UUID NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    citation_number INT NOT NULL,
    citation_key VARCHAR(50) NOT NULL,
    formatted_ieee TEXT NOT NULL,
    formatted_apa TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_citations_report_id ON citations(report_id);
CREATE INDEX idx_citations_source_id ON citations(source_id);

-- Artifacts
CREATE TABLE artifacts (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES research_runs(id) ON DELETE CASCADE,
    artifact_type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_artifacts_run_id ON artifacts(run_id);

-- Scheduled Research
CREATE TABLE scheduled_research (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    topic TEXT NOT NULL,
    cron_expression VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    depth VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    last_run_at TIMESTAMP,
    next_run_at TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_scheduled_research_project_id ON scheduled_research(project_id);
CREATE INDEX idx_scheduled_research_active ON scheduled_research(active);

-- Notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);

-- Audit Logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
