const BASE_URL = "";

const state = {
    token: localStorage.getItem("ttm_token"),
    user: readStoredJson("ttm_user"),
    loginType: "member",
    dashboard: null,
    projects: [],
    users: [],
    taskPage: null,
    selectedProjectId: null,
    taskView: "all",
    filters: {
        search: "",
        status: "",
        priority: "",
        projectId: "",
        page: 0,
        size: 8
    }
};

const elements = {
    authShell: document.getElementById("auth-shell"),
    appShell: document.getElementById("app-shell"),
    loginTab: document.getElementById("login-tab"),
    signupTab: document.getElementById("signup-tab"),
    loginEntryRow: document.getElementById("login-entry-row"),
    memberLoginLink: document.getElementById("member-login-link"),
    adminLoginLink: document.getElementById("admin-login-link"),
    authPanelLabel: document.getElementById("auth-panel-label"),
    authPanelTitle: document.getElementById("auth-panel-title"),
    authPanelNote: document.getElementById("auth-panel-note"),
    authStatusBanner: document.getElementById("auth-status-banner"),
    loginForm: document.getElementById("login-form"),
    loginSubmitButton: document.getElementById("login-submit-button"),
    signupForm: document.getElementById("signup-form"),
    appStatusBanner: document.getElementById("app-status-banner"),
    dashboardTitle: document.getElementById("dashboard-title"),
    metricGrid: document.getElementById("metric-grid"),
    statusGrid: document.getElementById("status-grid"),
    projectList: document.getElementById("project-list"),
    projectForm: document.getElementById("project-form"),
    projectFormTitle: document.getElementById("project-form-title"),
    projectName: document.getElementById("project-name"),
    projectDescription: document.getElementById("project-description"),
    projectSubmitButton: document.getElementById("project-submit-button"),
    projectDeleteButton: document.getElementById("project-delete-button"),
    newProjectButton: document.getElementById("new-project-button"),
    teamPanel: document.getElementById("team-panel"),
    taskSectionTitle: document.getElementById("task-section-title"),
    taskViewButtons: Array.from(document.querySelectorAll("[data-task-view]")),
    taskSearch: document.getElementById("task-search"),
    taskFilterStatus: document.getElementById("task-filter-status"),
    taskFilterPriority: document.getElementById("task-filter-priority"),
    taskFilterProject: document.getElementById("task-filter-project"),
    taskFilterApply: document.getElementById("task-filter-apply"),
    taskFilterReset: document.getElementById("task-filter-reset"),
    taskForm: document.getElementById("task-form"),
    taskTitle: document.getElementById("task-title"),
    taskDescription: document.getElementById("task-description"),
    taskDueDate: document.getElementById("task-due-date"),
    taskStatus: document.getElementById("task-status"),
    taskPriority: document.getElementById("task-priority"),
    taskProject: document.getElementById("task-project"),
    taskAssignee: document.getElementById("task-assignee"),
    taskSubmitButton: document.getElementById("task-submit-button"),
    taskListTitle: document.getElementById("task-list-title"),
    taskList: document.getElementById("task-list"),
    taskPagination: document.getElementById("task-pagination"),
    userDirectory: document.getElementById("user-directory"),
    currentUserName: document.getElementById("current-user-name"),
    currentUserRole: document.getElementById("current-user-role"),
    workspaceTitle: document.getElementById("workspace-title"),
    refreshButton: document.getElementById("refresh-button"),
    logoutButton: document.getElementById("logout-button")
};

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initialize);
} else {
    initialize();
}

function initialize() {
    bindEvents();
    handleRouteChange();
}

function bindEvents() {
    elements.loginTab?.addEventListener("click", () => navigateTo(`/login?type=${state.loginType}`));
    elements.signupTab?.addEventListener("click", () => navigateTo("/signup"));
    elements.memberLoginLink?.addEventListener("click", handleLoginEntryNavigation);
    elements.adminLoginLink?.addEventListener("click", handleLoginEntryNavigation);
    elements.loginForm?.addEventListener("submit", handleLogin);
    elements.signupForm?.addEventListener("submit", handleSignup);
    elements.projectForm?.addEventListener("submit", handleProjectSubmit);
    elements.newProjectButton?.addEventListener("click", resetProjectEditor);
    elements.projectDeleteButton?.addEventListener("click", handleProjectDelete);
    elements.taskForm?.addEventListener("submit", handleTaskCreate);
    elements.taskProject?.addEventListener("change", syncTaskAssignees);
    elements.refreshButton?.addEventListener("click", () => refreshWorkspace(false));
    elements.logoutButton?.addEventListener("click", logout);
    elements.taskFilterApply?.addEventListener("click", applyTaskFilters);
    elements.taskFilterReset?.addEventListener("click", resetTaskFilters);

    elements.taskViewButtons.forEach(button => {
        button.addEventListener("click", () => {
            state.taskView = button.dataset.taskView;
            state.filters.page = 0;
            renderTaskViewButtons();
            loadTasks(true);
        });
    });

    window.addEventListener("popstate", handleRouteChange);
}

function handleLoginEntryNavigation(event) {
    event.preventDefault();
    navigateTo(event.currentTarget.getAttribute("href"));
}

function handleRouteChange() {
    const route = getRouteState();

    if (route.path === "/") {
        window.history.replaceState({}, "", "/login?type=member");
        state.loginType = "member";
    } else if (route.path !== "/login" && route.path !== "/signup" && route.path !== "/admin-dashboard" && route.path !== "/member-dashboard") {
        window.history.replaceState({}, "", "/login?type=member");
        state.loginType = "member";
    } else {
        syncRouteState();
    }

    if (state.token) {
        if (state.user) {
            ensureAuthorizedRoute();
            showAppView();
            return;
        }

        bootstrapSession();
        return;
    }

    renderPublicRoute();
}

function syncRouteState() {
    state.loginType = resolveLoginType(new URLSearchParams(window.location.search).get("type"));
}

function renderPublicRoute() {
    const route = getRouteState();
    const loginType = route.path === "/login" ? route.loginType : "member";
    const mode = route.path === "/signup" ? "signup" : "login";
    showAuthView(mode, loginType);
}

async function bootstrapSession() {
    try {
        state.user = await apiRequest("/users/me");
        storeSession(state.token, state.user);
        await refreshWorkspace(true);
        ensureAuthorizedRoute();
        showAppView();
    } catch (error) {
        const fallbackLoginType = state.user?.role === "ADMIN" ? "admin" : "member";
        clearSession();
        showBanner(error.message || "Session expired.", "error");
        redirectToLogin(fallbackLoginType, true);
    }
}

async function refreshWorkspace(silent) {
    try {
        const requests = [
            apiRequest("/dashboard"),
            apiRequest("/projects"),
            state.user.role === "ADMIN" ? apiRequest("/users") : Promise.resolve([])
        ];

        const [dashboard, projects, users] = await Promise.all(requests);

        state.dashboard = dashboard;
        state.projects = projects;
        state.users = users;

        if (!state.selectedProjectId || !state.projects.some(project => project.id === state.selectedProjectId)) {
            state.selectedProjectId = state.projects[0]?.id ?? null;
        }

        adjustTaskViewForRole();
        renderStaticSections();
        await loadTasks(true);
        showAppView();

        if (!silent) {
            showBanner("Workspace refreshed.", "success");
        }
    } catch (error) {
        handleRequestFailure(error, "Unable to refresh workspace.");
    }
}

async function handleLogin(event) {
    event.preventDefault();

    const email = document.getElementById("login-email")?.value.trim() || "";
    const password = document.getElementById("login-password")?.value || "";
    const loginPath = `/auth/login?type=${state.loginType}`;

    try {
        console.log("Submitting login request", {
            path: BASE_URL + loginPath,
            loginType: state.loginType,
            email
        });

        const response = await apiRequest(loginPath, {
            method: "POST",
            body: JSON.stringify({
                email,
                password
            })
        }, false);

        console.log("Login response received", response);
        state.token = response.accessToken;
        state.user = response.user;
        localStorage.setItem("ttm_token", response.accessToken);
        localStorage.setItem("ttm_user", JSON.stringify(response.user));
        await refreshWorkspace(true);
        redirectToDashboard(response.user.role, true);
        showBanner("Logged in successfully.", "success");
    } catch (error) {
        showBanner(error.message || "Login failed.", "error");
    }
}

async function handleSignup(event) {
    event.preventDefault();

    try {
        const response = await apiRequest("/auth/signup", {
            method: "POST",
            body: JSON.stringify({
                fullName: document.getElementById("signup-name").value.trim(),
                email: document.getElementById("signup-email").value.trim(),
                password: document.getElementById("signup-password").value
            })
        }, false);

        state.token = response.accessToken;
        state.user = response.user;
        storeSession(response.accessToken, response.user);
        await refreshWorkspace(true);
        redirectToDashboard(response.user.role, true);
        showBanner("Account created successfully.", "success");
    } catch (error) {
        showBanner(error.message || "Signup failed.", "error");
    }
}

async function handleProjectSubmit(event) {
    event.preventDefault();

    const payload = {
        name: elements.projectName.value.trim(),
        description: elements.projectDescription.value.trim()
    };

    try {
        if (state.selectedProjectId) {
            await apiRequest(`/projects/${state.selectedProjectId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            showBanner("Project updated successfully.", "success");
        } else {
            const response = await apiRequest("/projects", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            state.selectedProjectId = response.id;
            showBanner("Project created successfully.", "success");
        }

        await refreshWorkspace(true);
    } catch (error) {
        showBanner(error.message || "Project request failed.", "error");
    }
}

async function handleProjectDelete() {
    if (!state.selectedProjectId) {
        showBanner("Select a project before deleting it.", "error");
        return;
    }

    if (!window.confirm("Delete the selected project?")) {
        return;
    }

    try {
        await apiRequest(`/projects/${state.selectedProjectId}`, {
            method: "DELETE"
        });
        state.selectedProjectId = null;
        resetProjectEditor();
        await refreshWorkspace(true);
        showBanner("Project deleted successfully.", "success");
    } catch (error) {
        showBanner(error.message || "Project could not be deleted.", "error");
    }
}

async function handleTaskCreate(event) {
    event.preventDefault();

    try {
        await apiRequest("/tasks", {
            method: "POST",
            body: JSON.stringify({
                title: elements.taskTitle.value.trim(),
                description: elements.taskDescription.value.trim(),
                dueDate: elements.taskDueDate.value,
                status: elements.taskStatus.value,
                priority: elements.taskPriority.value,
                projectId: Number(elements.taskProject.value),
                assigneeId: Number(elements.taskAssignee.value)
            })
        });

        elements.taskForm.reset();
        elements.taskStatus.value = "TODO";
        elements.taskPriority.value = "MEDIUM";
        syncTaskAssignees();
        await refreshWorkspace(true);
        showBanner("Task created successfully.", "success");
    } catch (error) {
        showBanner(error.message || "Task could not be created.", "error");
    }
}

async function handleAddMembers(projectId) {
    const selectedBoxes = Array.from(document.querySelectorAll(".member-checkbox:checked"));
    const memberIds = selectedBoxes.map(input => Number(input.value));

    if (!memberIds.length) {
        showBanner("Select at least one member to add.", "error");
        return;
    }

    try {
        await apiRequest(`/projects/${projectId}/add-member`, {
            method: "PUT",
            body: JSON.stringify({ memberIds })
        });

        await refreshWorkspace(true);
        showBanner("Members added successfully.", "success");
    } catch (error) {
        showBanner(error.message || "Members could not be added.", "error");
    }
}

async function handleRemoveMember(projectId, memberId) {
    try {
        await apiRequest(`/projects/${projectId}/members/${memberId}`, {
            method: "DELETE"
        });

        await refreshWorkspace(true);
        showBanner("Member removed successfully.", "success");
    } catch (error) {
        showBanner(error.message || "Member could not be removed.", "error");
    }
}

async function handleAssignTask(taskId) {
    const select = document.querySelector(`[data-task-assignee='${taskId}']`);

    if (!select) {
        return;
    }

    try {
        await apiRequest(`/tasks/${taskId}/assign`, {
            method: "PUT",
            body: JSON.stringify({ assigneeId: Number(select.value) })
        });

        await loadTasks(true);
        showBanner("Task reassigned successfully.", "success");
    } catch (error) {
        showBanner(error.message || "Task could not be reassigned.", "error");
    }
}

async function handleStatusUpdate(taskId, nextStatus) {
    try {
        await apiRequest(`/tasks/${taskId}/status`, {
            method: "PUT",
            body: JSON.stringify({ status: nextStatus })
        });

        await refreshWorkspace(true);
        showBanner("Task status updated.", "success");
    } catch (error) {
        showBanner(error.message || "Status update failed.", "error");
        await loadTasks(true);
    }
}

function applyTaskFilters() {
    state.filters.search = elements.taskSearch.value.trim();
    state.filters.status = elements.taskFilterStatus.value;
    state.filters.priority = elements.taskFilterPriority.value;
    state.filters.projectId = elements.taskFilterProject.value;
    state.filters.page = 0;
    loadTasks(true);
}

function resetTaskFilters() {
    elements.taskSearch.value = "";
    elements.taskFilterStatus.value = "";
    elements.taskFilterPriority.value = "";
    elements.taskFilterProject.value = "";
    state.filters.search = "";
    state.filters.status = "";
    state.filters.priority = "";
    state.filters.projectId = "";
    state.filters.page = 0;
    loadTasks(true);
}

async function loadTasks(silent) {
    try {
        const path = getTaskPath();
        const queryString = new URLSearchParams({
            page: String(state.filters.page),
            size: String(state.filters.size)
        });

        if (state.filters.search) {
            queryString.set("search", state.filters.search);
        }
        if (state.filters.status) {
            queryString.set("status", state.filters.status);
        }
        if (state.filters.priority) {
            queryString.set("priority", state.filters.priority);
        }
        if (state.filters.projectId) {
            queryString.set("projectId", state.filters.projectId);
        }

        state.taskPage = await apiRequest(`${path}?${queryString.toString()}`);
        renderTaskSection();

        if (!silent) {
            showBanner("Tasks refreshed.", "success");
        }
    } catch (error) {
        handleRequestFailure(error, "Unable to load tasks.");
    }
}

function renderStaticSections() {
    renderTopbar();
    renderDashboard();
    renderProjects();
    renderProjectEditor();
    renderTaskViewButtons();
    renderTaskFilters();
    renderTaskForm();
    renderUserDirectory();
}

function renderTopbar() {
    elements.currentUserName.textContent = state.user.fullName;
    elements.currentUserRole.textContent = state.user.role;
    elements.workspaceTitle.textContent = state.user.role === "ADMIN" ? "Admin Dashboard" : "Member Dashboard";
    elements.dashboardTitle.textContent = state.user.role === "ADMIN" ? "Admin delivery overview" : "Member delivery overview";
    elements.taskSectionTitle.textContent = state.user.role === "ADMIN"
        ? "Task assignment and tracking"
        : "Assigned task tracking";

    document.querySelectorAll(".admin-only").forEach(element => {
        element.classList.toggle("is-hidden", state.user.role !== "ADMIN");
    });
}

function renderDashboard() {
    const dashboard = state.dashboard;
    const statusCounts = dashboard?.tasksByStatus ?? [];
    const totalTasks = dashboard?.totalTasks ?? 0;
    const overdueTasks = dashboard?.overdueTasks ?? 0;
    const doneCount = statusCounts.find(item => item.status === "DONE")?.count ?? 0;
    const completionRate = totalTasks === 0 ? 0 : Math.round((doneCount / totalTasks) * 100);

    elements.metricGrid.innerHTML = [
        createMetricCard(state.user.role === "ADMIN" ? "Total tasks" : "Assigned tasks", totalTasks),
        createMetricCard("Done", doneCount),
        createMetricCard("Overdue", overdueTasks)
    ].join("");

    elements.statusGrid.innerHTML = statusCounts.map(item => {
        const percentage = totalTasks === 0 ? 0 : Math.round((item.count / totalTasks) * 100);
        return `
            <article class="status-card">
                <div>
                    <p class="section-label">${formatLabel(item.status)}</p>
                    <h4>${item.count}</h4>
                </div>
                <div class="status-bar">
                    <div class="status-bar-fill" style="width:${percentage}%"></div>
                </div>
                <p>${percentage}% of tracked workload</p>
            </article>
        `;
    }).join("") + `
        <article class="status-card">
            <div>
                <p class="section-label">Completion rate</p>
                <h4>${completionRate}%</h4>
            </div>
            <div class="status-bar">
                <div class="status-bar-fill" style="width:${completionRate}%"></div>
            </div>
            <p>Based on DONE tasks in the current dashboard scope.</p>
        </article>
    `;
}

function renderProjects() {
    if (!state.projects.length) {
        elements.projectList.innerHTML = `<p class="empty-state">No projects available.</p>`;
        return;
    }

    elements.projectList.innerHTML = state.projects.map(project => {
        const selected = project.id === state.selectedProjectId ? "is-selected" : "";
        return `
            <button class="project-item ${selected}" type="button" data-project-select="${project.id}">
                <div class="project-item-header">
                    <div>
                        <h4>${escapeHtml(project.name)}</h4>
                        <div class="project-meta">
                            <span>${project.memberCount} members</span>
                            <span>${project.taskCount} tasks</span>
                            <span>Owner: ${escapeHtml(project.createdBy.fullName)}</span>
                        </div>
                    </div>
                    <span class="role-badge">${project.progressPercentage}%</span>
                </div>
                <p class="project-meta">${escapeHtml(project.description || "No description provided.")}</p>
                <div class="project-progress">
                    <div class="progress-track">
                        <div class="progress-fill" style="width:${project.progressPercentage}%"></div>
                    </div>
                    <span class="muted">${project.completedTaskCount}/${project.taskCount} tasks done</span>
                </div>
            </button>
        `;
    }).join("");

    document.querySelectorAll("[data-project-select]").forEach(button => {
        button.addEventListener("click", () => {
            state.selectedProjectId = Number(button.dataset.projectSelect);
            renderProjects();
            renderProjectEditor();
            renderTaskForm();
        });
    });
}

function renderProjectEditor() {
    const project = getSelectedProject();

    if (state.user.role === "ADMIN") {
        if (project) {
            elements.projectFormTitle.textContent = "Edit project";
            elements.projectName.value = project.name;
            elements.projectDescription.value = project.description || "";
            elements.projectSubmitButton.textContent = "Save changes";
            elements.projectDeleteButton.disabled = false;
        } else {
            elements.projectFormTitle.textContent = "Create project";
            elements.projectName.value = "";
            elements.projectDescription.value = "";
            elements.projectSubmitButton.textContent = "Create project";
            elements.projectDeleteButton.disabled = true;
        }
    }

    renderTeamPanel();
}

function renderTeamPanel() {
    const project = getSelectedProject();

    if (!project) {
        elements.teamPanel.innerHTML = `<p class="empty-state">Select a project to inspect team members.</p>`;
        return;
    }

    const membersHtml = project.members.map(member => `
        <div class="member-item">
            <div class="member-row">
                <div>
                    <strong>${escapeHtml(member.fullName)}</strong>
                    <div class="user-meta">
                        <span>${escapeHtml(member.email)}</span>
                        <span>${member.role}</span>
                    </div>
                </div>
                ${state.user.role === "ADMIN" && project.createdBy.id !== member.id ? `
                    <button class="danger-button" type="button" data-member-remove="${member.id}">Remove</button>
                ` : ""}
            </div>
        </div>
    `).join("");

    const availableUsers = state.users.filter(user => !project.members.some(member => member.id === user.id));
    const addMemberHtml = state.user.role === "ADMIN" ? `
        <div class="member-grid">
            <h4>Add members</h4>
            ${availableUsers.length ? `
                <div class="member-selector">
                    ${availableUsers.map(user => `
                        <label class="checkbox-item">
                            <input class="member-checkbox" type="checkbox" value="${user.id}">
                            <span>
                                <strong>${escapeHtml(user.fullName)}</strong><br>
                                <span class="muted">${escapeHtml(user.email)} - ${user.role}</span>
                            </span>
                        </label>
                    `).join("")}
                </div>
                <button class="primary-button" id="add-members-button" type="button">Add selected members</button>
            ` : `<p class="empty-state">All registered users are already part of this project.</p>`}
        </div>
    ` : "";

    elements.teamPanel.innerHTML = `
        <div class="stack-form">
            <div class="surface-header">
                <h4>${escapeHtml(project.name)} team</h4>
            </div>
            ${membersHtml}
            ${addMemberHtml}
        </div>
    `;

    document.querySelectorAll("[data-member-remove]").forEach(button => {
        button.addEventListener("click", () => handleRemoveMember(project.id, Number(button.dataset.memberRemove)));
    });

    const addMembersButton = document.getElementById("add-members-button");
    if (addMembersButton) {
        addMembersButton.addEventListener("click", () => handleAddMembers(project.id));
    }
}

function renderTaskViewButtons() {
    elements.taskViewButtons.forEach(button => {
        const view = button.dataset.taskView;
        const shouldHide = state.user.role === "ADMIN" ? view === "my" : view === "all";
        button.classList.toggle("is-hidden", shouldHide);
        button.classList.toggle("is-active", state.taskView === view);
    });

    elements.taskListTitle.textContent = state.user.role === "ADMIN"
        ? (state.taskView === "overdue" ? "Overdue tasks" : "All tasks")
        : (state.taskView === "overdue" ? "Personal overdue tasks" : "My tasks");
}

function renderTaskFilters() {
    elements.taskSearch.value = state.filters.search;
    elements.taskFilterStatus.value = state.filters.status;
    elements.taskFilterPriority.value = state.filters.priority;

    const projectOptions = ['<option value="">All</option>'].concat(
        state.projects.map(project => `<option value="${project.id}">${escapeHtml(project.name)}</option>`)
    );
    elements.taskFilterProject.innerHTML = projectOptions.join("");
    elements.taskFilterProject.value = state.filters.projectId;
}

function renderTaskForm() {
    if (state.user.role !== "ADMIN") {
        return;
    }

    if (!state.projects.length) {
        elements.taskProject.innerHTML = `<option value="">No projects available</option>`;
        elements.taskAssignee.innerHTML = `<option value="">No assignees available</option>`;
        elements.taskSubmitButton.disabled = true;
        return;
    }

    elements.taskProject.innerHTML = state.projects.map(project => `
        <option value="${project.id}">${escapeHtml(project.name)}</option>
    `).join("");

    if (state.selectedProjectId) {
        elements.taskProject.value = String(state.selectedProjectId);
    }

    elements.taskSubmitButton.disabled = false;
    syncTaskAssignees();
}

function syncTaskAssignees() {
    if (state.user?.role !== "ADMIN") {
        return;
    }

    const projectId = Number(elements.taskProject.value);
    const project = state.projects.find(item => item.id === projectId);

    if (!project) {
        elements.taskAssignee.innerHTML = `<option value="">No assignees available</option>`;
        elements.taskSubmitButton.disabled = true;
        return;
    }

    elements.taskAssignee.innerHTML = project.members.map(member => `
        <option value="${member.id}">${escapeHtml(member.fullName)} (${member.role})</option>
    `).join("");
    elements.taskSubmitButton.disabled = project.members.length === 0;
}

function renderTaskSection() {
    renderTaskList();
    renderPagination();
}

function renderTaskList() {
    const tasks = state.taskPage?.content ?? [];

    if (!tasks.length) {
        elements.taskList.innerHTML = `<p class="empty-state">No tasks found for the current filters.</p>`;
        return;
    }

    elements.taskList.innerHTML = tasks.map(task => {
        const project = state.projects.find(item => item.id === task.projectId);
        const dueDate = new Date(task.dueDate).toLocaleDateString();
        const canUpdateStatus = state.user.role === "MEMBER" && task.assignee.id === state.user.id;
        const adminAssignControl = state.user.role === "ADMIN" ? createAssignControl(task, project) : "";

        return `
            <article class="task-item">
                <div class="task-item-header">
                    <div>
                        <h4>${escapeHtml(task.title)}</h4>
                        <div class="task-meta">
                            <span>${escapeHtml(task.projectName)}</span>
                            <span>Assignee: ${escapeHtml(task.assignee.fullName)}</span>
                            <span>Due ${dueDate}</span>
                        </div>
                    </div>
                    <div class="task-controls">
                        ${task.overdue ? '<span class="overdue-badge">Overdue</span>' : ""}
                        <span class="priority-pill ${task.priority.toLowerCase()}">${task.priority}</span>
                        <span class="status-pill ${task.status.toLowerCase().replace("_", "-")}">${formatLabel(task.status)}</span>
                    </div>
                </div>
                <p class="task-meta">${escapeHtml(task.description || "No description provided.")}</p>
                <div class="task-item-header">
                    <div class="task-meta">
                        <span>Created by ${escapeHtml(task.createdBy.fullName)}</span>
                    </div>
                    <div class="task-controls">
                        ${adminAssignControl}
                        ${canUpdateStatus ? createStatusControl(task) : ""}
                    </div>
                </div>
            </article>
        `;
    }).join("");

    document.querySelectorAll("[data-task-assign-button]").forEach(button => {
        button.addEventListener("click", () => handleAssignTask(Number(button.dataset.taskAssignButton)));
    });

    document.querySelectorAll("[data-task-status]").forEach(select => {
        select.addEventListener("change", () => handleStatusUpdate(Number(select.dataset.taskStatus), select.value));
    });
}

function renderPagination() {
    const taskPage = state.taskPage;

    if (!taskPage) {
        elements.taskPagination.innerHTML = "";
        return;
    }

    elements.taskPagination.innerHTML = `
        <span class="muted">Page ${taskPage.page + 1} of ${Math.max(taskPage.totalPages, 1)} - ${taskPage.totalElements} tasks</span>
        <div class="button-row">
            <button class="secondary-button" id="previous-page-button" type="button" ${taskPage.first ? "disabled" : ""}>Previous</button>
            <button class="secondary-button" id="next-page-button" type="button" ${taskPage.last ? "disabled" : ""}>Next</button>
        </div>
    `;

    const previousButton = document.getElementById("previous-page-button");
    const nextButton = document.getElementById("next-page-button");

    if (previousButton) {
        previousButton.addEventListener("click", () => {
            state.filters.page = Math.max(state.filters.page - 1, 0);
            loadTasks(true);
        });
    }

    if (nextButton) {
        nextButton.addEventListener("click", () => {
            state.filters.page += 1;
            loadTasks(true);
        });
    }
}

function renderUserDirectory() {
    if (state.user.role !== "ADMIN") {
        return;
    }

    if (!state.users.length) {
        elements.userDirectory.innerHTML = `<p class="empty-state">No users registered yet.</p>`;
        return;
    }

    elements.userDirectory.innerHTML = state.users.map(user => `
        <article class="user-item">
            <div class="user-item-header">
                <div>
                    <h4>${escapeHtml(user.fullName)}</h4>
                    <div class="user-meta">
                        <span>${escapeHtml(user.email)}</span>
                        <span>${user.role}</span>
                    </div>
                </div>
                <span class="role-badge">${user.role}</span>
            </div>
        </article>
    `).join("");
}

function createMetricCard(label, value) {
    return `
        <article class="metric-card">
            <p class="section-label">${label}</p>
            <strong>${value}</strong>
        </article>
    `;
}

function createAssignControl(task, project) {
    if (!project) {
        return "";
    }

    return `
        <select class="task-status-select" data-task-assignee="${task.id}">
            ${project.members.map(member => `
                <option value="${member.id}" ${member.id === task.assignee.id ? "selected" : ""}>
                    ${escapeHtml(member.fullName)}
                </option>
            `).join("")}
        </select>
        <button class="secondary-button" data-task-assign-button="${task.id}" type="button">Assign</button>
    `;
}

function createStatusControl(task) {
    const allowedStatuses = getAllowedStatuses(task.status);
    return `
        <select class="task-status-select" data-task-status="${task.id}">
            ${allowedStatuses.map(status => `
                <option value="${status}" ${status === task.status ? "selected" : ""}>${formatLabel(status)}</option>
            `).join("")}
        </select>
    `;
}

function adjustTaskViewForRole() {
    if (state.user.role === "MEMBER" && state.taskView === "all") {
        state.taskView = "my";
    }

    if (state.user.role === "ADMIN" && state.taskView === "my") {
        state.taskView = "all";
    }
}

function getTaskPath() {
    if (state.user.role === "ADMIN") {
        return state.taskView === "overdue" ? "/tasks/overdue" : "/tasks";
    }

    return state.taskView === "overdue" ? "/tasks/overdue" : "/tasks/my-tasks";
}

function getSelectedProject() {
    return state.projects.find(project => project.id === state.selectedProjectId) ?? null;
}

function resetProjectEditor() {
    state.selectedProjectId = null;
    elements.projectForm.reset();
    elements.projectFormTitle.textContent = "Create project";
    elements.projectSubmitButton.textContent = "Create project";
    elements.projectDeleteButton.disabled = true;
    renderProjects();
    renderProjectEditor();
}

function logout() {
    const loginType = state.user?.role === "ADMIN" ? "admin" : "member";

    clearSession();
    state.dashboard = null;
    state.projects = [];
    state.users = [];
    state.taskPage = null;
    state.selectedProjectId = null;
    state.taskView = "all";
    state.filters = {
        search: "",
        status: "",
        priority: "",
        projectId: "",
        page: 0,
        size: 8
    };
    redirectToLogin(loginType, true);
    showBanner("Logged out.", "success");
}

function showAuthView(mode, loginType = "member") {
    state.loginType = resolveLoginType(loginType);
    elements.authShell.classList.remove("is-hidden");
    elements.appShell.classList.add("is-hidden");
    hideBanner(elements.appStatusBanner);
    elements.loginTab.classList.toggle("is-active", mode === "login");
    elements.signupTab.classList.toggle("is-active", mode === "signup");
    elements.loginForm.classList.toggle("is-hidden", mode !== "login");
    elements.signupForm.classList.toggle("is-hidden", mode !== "signup");
    elements.loginEntryRow.classList.toggle("is-hidden", mode !== "login");

    elements.memberLoginLink.classList.toggle("is-active", state.loginType === "member");
    elements.adminLoginLink.classList.toggle("is-active", state.loginType === "admin");

    if (mode === "signup") {
        elements.authPanelLabel.textContent = "Member onboarding";
        elements.authPanelTitle.textContent = "Create member account";
        elements.authPanelNote.textContent = "New signups create member accounts only. Admin access is assigned from the backend.";
        return;
    }

    const isAdminLogin = state.loginType === "admin";
    elements.authPanelLabel.textContent = isAdminLogin ? "Admin access" : "Member access";
    elements.authPanelTitle.textContent = isAdminLogin ? "Admin login" : "Member login";
    elements.authPanelNote.textContent = isAdminLogin
        ? "Use an administrator account to manage projects, teams, and task assignments."
        : "Use your member account credentials to view and update assigned tasks.";
    elements.loginSubmitButton.textContent = isAdminLogin ? "Login as Admin" : "Login as Member";
}

function showAppView() {
    elements.authShell.classList.add("is-hidden");
    elements.appShell.classList.remove("is-hidden");
    hideBanner(elements.authStatusBanner);
}

function navigateTo(path) {
    window.history.pushState({}, "", path);
    handleRouteChange();
}

function replaceRoute(path) {
    window.history.replaceState({}, "", path);
    handleRouteChange();
}

function getRouteState() {
    const path = window.location.pathname === "" ? "/" : window.location.pathname;
    const query = new URLSearchParams(window.location.search);

    if (path === "/login") {
        return {
            view: "auth",
            mode: "login",
            loginType: resolveLoginType(query.get("type")),
            path
        };
    }

    if (path === "/signup") {
        return {
            view: "auth",
            mode: "signup",
            loginType: "member",
            path
        };
    }

    if (path === "/admin-dashboard") {
        return { view: "app", role: "ADMIN", path };
    }

    if (path === "/member-dashboard") {
        return { view: "app", role: "MEMBER", path };
    }

    return {
        view: "auth",
        mode: "login",
        loginType: "member",
        path
    };
}

function resolveLoginType(value) {
    return String(value || "").toLowerCase() === "admin" ? "admin" : "member";
}

function ensureAuthorizedRoute() {
    if (!state.user) {
        return;
    }

    const route = getRouteState();
    const expectedPath = getDashboardPathForRole(state.user.role);

    if (route.path !== expectedPath) {
        window.history.replaceState({}, "", expectedPath);
    }
}

function redirectToLogin(loginType = "member", replace = false) {
    const targetPath = `/login?type=${resolveLoginType(loginType)}`;

    if (replace) {
        replaceRoute(targetPath);
        return;
    }

    navigateTo(targetPath);
}

function redirectToDashboard(role, replace = false) {
    const targetPath = getDashboardPathForRole(role);

    if (replace) {
        replaceRoute(targetPath);
        return;
    }

    navigateTo(targetPath);
}

function getDashboardPathForRole(role) {
    return role === "ADMIN" ? "/admin-dashboard" : "/member-dashboard";
}

function showBanner(message, type) {
    const activeBanner = elements.appShell.classList.contains("is-hidden")
        ? elements.authStatusBanner
        : elements.appStatusBanner;
    const inactiveBanner = activeBanner === elements.authStatusBanner
        ? elements.appStatusBanner
        : elements.authStatusBanner;

    hideBanner(inactiveBanner);
    activeBanner.textContent = message;
    activeBanner.classList.remove("is-hidden", "is-success", "is-error");
    activeBanner.classList.add(type === "success" ? "is-success" : "is-error");

    clearTimeout(showBanner.timeoutId);
    showBanner.timeoutId = window.setTimeout(() => {
        hideBanner(activeBanner);
    }, 4000);
}

function hideBanner(element) {
    if (!element) {
        return;
    }

    element.classList.add("is-hidden");
    element.classList.remove("is-success", "is-error");
}

async function apiRequest(path, options = {}, requiresAuth = true) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    if (requiresAuth && state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    const response = await fetch(BASE_URL + path, {
        ...options,
        headers
    });

    const rawBody = await response.text();
    let data = null;

    if (rawBody) {
        try {
            data = JSON.parse(rawBody);
        } catch (error) {
            data = null;
        }
    }

    if (!response.ok) {
        const error = new Error(
            data?.fieldErrors?.length
                ? data.fieldErrors.map(item => item.message).join(", ")
                : data?.message || "Request failed."
        );
        error.status = response.status;
        throw error;
    }

    return data;
}

function handleRequestFailure(error, fallbackMessage) {
    if (error.status === 401) {
        logout();
        return;
    }

    showBanner(error.message || fallbackMessage, "error");
}

function clearSession() {
    state.token = null;
    state.user = null;
    localStorage.removeItem("ttm_token");
    localStorage.removeItem("ttm_user");
}

function storeSession(token, user) {
    localStorage.setItem("ttm_token", token);
    localStorage.setItem("ttm_user", JSON.stringify(user));
}

function readStoredJson(key) {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : null;
}

function getAllowedStatuses(currentStatus) {
    if (currentStatus === "TODO") {
        return ["TODO", "IN_PROGRESS"];
    }

    if (currentStatus === "IN_PROGRESS") {
        return ["IN_PROGRESS", "DONE"];
    }

    return ["DONE"];
}

function formatLabel(value) {
    return value
        .toLowerCase()
        .split("_")
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
