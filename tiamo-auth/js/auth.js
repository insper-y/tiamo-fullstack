/* ============================================================
   Tiamo AI 认证系统 - 交互逻辑 + API 封装
   ============================================================ */

(function () {
    'use strict';

    /* ========== 全局配置（从 config.js 读取） ========== */
    var CONFIG = window.APP_CONFIG || {};
    var API_BASE = CONFIG.API_BASE || 'http://localhost:8080';
    var TOKEN_KEY = CONFIG.TOKEN_KEY || 'tiamo_token';
    var USER_KEY = CONFIG.USER_KEY || 'tiamo_user';

    /* ========== API 封装 ========== */
    var Api = {
        /**
         * 通用请求方法
         */
        request: function (method, url, data) {
            var options = {
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                }
            };

            // 自动携带 Token
            var token = Auth.getToken();
            if (token) {
                options.headers['Authorization'] = 'Bearer ' + token;
            }

            if (data && method !== 'GET') {
                options.body = JSON.stringify(data);
            }

            return fetch(API_BASE + url, options)
                .then(function (response) {
                    return response.json().then(function (result) {
                        result.httpStatus = response.status;
                        return result;
                    });
                })
                .catch(function (error) {
                    console.error('API请求失败:', error);
                    return { code: -1, msg: '网络请求失败，请检查后端服务是否启动' };
                });
        },

        get: function (url) { return this.request('GET', url); },
        post: function (url, data) { return this.request('POST', url, data); },
        put: function (url, data) { return this.request('PUT', url, data); },
        del: function (url) { return this.request('DELETE', url); },

        /* ---- 认证相关接口 ---- */

        /** 登录 */
        login: function (username, password, remember) {
            return this.post('/api/auth/login', {
                username: username,
                password: password,
                remember: remember
            });
        },

        /** 注册 */
        register: function (data) {
            return this.post('/api/auth/register', data);
        },

        /** 发送验证码 */
        sendCaptcha: function (phone) {
            return this.post('/api/auth/send-captcha', { phone: phone });
        },

        /** 重置密码 */
        resetPassword: function (data) {
            return this.post('/api/auth/reset-password', data);
        },

        /** 验证Token */
        verifyToken: function () {
            return this.get('/api/auth/verify');
        },

        /* ---- 业务数据接口（需认证） ---- */

        /** 获取商品列表 */
        getBooks: function () {
            return this.get('/maven/books');
        },

        /** 新增商品 */
        addBook: function (data) {
            return this.post('/maven/books/', data);
        },

        /** 修改商品 */
        updateBook: function (data) {
            return this.put('/maven/books/', data);
        },

        /** 删除商品 */
        deleteBook: function (id) {
            return this.del('/maven/books/' + id);
        }
    };

    /* ========== Token 管理 ========== */
    var Auth = {
        /** 保存 Token 和用户信息 */
        setAuth: function (token, userInfo) {
            localStorage.setItem(TOKEN_KEY, token);
            if (userInfo) {
                localStorage.setItem(USER_KEY, JSON.stringify(userInfo));
            }
        },

        /** 获取 Token */
        getToken: function () {
            return localStorage.getItem(TOKEN_KEY);
        },

        /** 获取当前用户信息 */
        getUser: function () {
            var userStr = localStorage.getItem(USER_KEY);
            return userStr ? JSON.parse(userStr) : null;
        },

        /** 退出登录 */
        logout: function () {
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(USER_KEY);
        },

        /** 检查是否已登录 */
        isLoggedIn: function () {
            return !!this.getToken();
        },

        /** 要求登录（未登录跳转登录页） */
        requireAuth: function () {
            if (!this.isLoggedIn()) {
                window.location.href = 'login.html';
                return false;
            }
            return true;
        }
    };

    // 暴露到全局
    window.Api = Api;
    window.Auth = Auth;

    /* ========== 粒子背景 ========== */
    function initParticles() {
        var canvas = document.getElementById('particles');
        if (!canvas) return;

        var ctx = canvas.getContext('2d');
        var particles = [];
        var particleCount = window.innerWidth < 768 ? 30 : 60;
        var animationId;

        function resize() {
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
        }

        function createParticle() {
            return {
                x: Math.random() * canvas.width,
                y: Math.random() * canvas.height,
                vx: (Math.random() - 0.5) * 0.5,
                vy: (Math.random() - 0.5) * 0.5,
                radius: Math.random() * 2 + 1,
                opacity: Math.random() * 0.5 + 0.2
            };
        }

        function init() {
            resize();
            particles = [];
            for (var i = 0; i < particleCount; i++) {
                particles.push(createParticle());
            }
        }

        function draw() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            for (var i = 0; i < particles.length; i++) {
                var p = particles[i];
                p.x += p.vx;
                p.y += p.vy;

                if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
                if (p.y < 0 || p.y > canvas.height) p.vy *= -1;

                ctx.beginPath();
                ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
                ctx.fillStyle = 'rgba(129, 140, 248, ' + p.opacity + ')';
                ctx.fill();
            }

            for (var i = 0; i < particles.length; i++) {
                for (var j = i + 1; j < particles.length; j++) {
                    var dx = particles[i].x - particles[j].x;
                    var dy = particles[i].y - particles[j].y;
                    var dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < 120) {
                        ctx.beginPath();
                        ctx.moveTo(particles[i].x, particles[i].y);
                        ctx.lineTo(particles[j].x, particles[j].y);
                        ctx.strokeStyle = 'rgba(99, 102, 241, ' + (0.15 * (1 - dist / 120)) + ')';
                        ctx.lineWidth = 0.5;
                        ctx.stroke();
                    }
                }
            }

            animationId = requestAnimationFrame(draw);
        }

        init();
        draw();

        window.addEventListener('resize', resize);
        document.addEventListener('visibilitychange', function () {
            if (document.hidden) {
                cancelAnimationFrame(animationId);
            } else {
                draw();
            }
        });
    }

    /* ========== 表单验证 ========== */
    var Validators = {
        required: function (v) { return v && v.trim().length > 0; },
        email: function (v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v); },
        phone: function (v) { return /^1[3-9]\d{9}$/.test(v); },
        username: function (v) { return /^[a-zA-Z0-9_]{4,20}$/.test(v); },
        password: function (v) { return v && v.length >= 6; },
        strongPassword: function (v) {
            return v && v.length >= 8 && /[a-z]/.test(v) && /[A-Z]/.test(v) && /\d/.test(v);
        },
        captcha: function (v) { return v && v.trim().length === 6; },
        match: function (v1, v2) { return v1 === v2; }
    };

    function validateField(input, rules) {
        var value = input.value;
        var group = input.closest('.form-group');

        for (var i = 0; i < rules.length; i++) {
            var rule = rules[i];
            var valid = true;
            var message = '';

            if (typeof rule === 'string') {
                valid = Validators[rule](value);
                message = getDefaultMessage(rule);
            } else if (rule.type === 'match') {
                var target = document.querySelector(rule.target);
                valid = Validators.match(value, target ? target.value : '');
                message = rule.message || '两次输入不一致';
            }

            if (!valid) {
                showError(group, message);
                return false;
            }
        }

        clearError(group);
        return true;
    }

    function getDefaultMessage(rule) {
        var messages = {
            required: '此项为必填',
            email: '请输入有效的邮箱地址',
            phone: '请输入有效的手机号',
            username: '用户名需为4-20位字母、数字或下划线',
            password: '密码至少6位',
            strongPassword: '密码需至少8位，包含大小写字母和数字',
            captcha: '请输入6位验证码'
        };
        return messages[rule] || '输入格式不正确';
    }

    function showError(group, message) {
        group.classList.add('has-error');
        var errorEl = group.querySelector('.error-message');
        if (errorEl) errorEl.textContent = message;
    }

    function clearError(group) {
        group.classList.remove('has-error');
    }

    /* ========== 密码显示切换 ========== */
    function initPasswordToggle() {
        document.querySelectorAll('.toggle-password').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var input = btn.parentElement.querySelector('.form-input');
                if (!input) return;
                var isPassword = input.type === 'password';
                input.type = isPassword ? 'text' : 'password';
                var eyeOpen = btn.querySelector('.eye-open');
                var eyeClosed = btn.querySelector('.eye-closed');
                if (eyeOpen && eyeClosed) {
                    eyeOpen.style.display = isPassword ? 'none' : 'block';
                    eyeClosed.style.display = isPassword ? 'block' : 'none';
                }
            });
        });
    }

    /* ========== 验证码倒计时（对接真实API） ========== */
    function initCaptchaCountdown() {
        document.querySelectorAll('.captcha-btn').forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (btn.disabled) return;

                var targetInput = document.querySelector(btn.dataset.target || '#phone');
                if (targetInput && !Validators[btn.dataset.validate || 'phone'](targetInput.value)) {
                    var group = targetInput.closest('.form-group');
                    showError(group, btn.dataset.validate === 'email' ? '请输入有效的邮箱' : '请输入有效的手机号');
                    return;
                }

                // 调用后端发送验证码接口
                btn.disabled = true;
                btn.textContent = '发送中...';

                Api.sendCaptcha(targetInput.value).then(function (res) {
                    if (res.code === 200) {
                        // 演示环境：如果后端返回了验证码，自动填充（生产环境删除）
                        if (res.data && res.data.captcha) {
                            var captchaInput = document.getElementById('captcha');
                            if (captchaInput) captchaInput.value = res.data.captcha;
                        }
                        showAlert('success', '验证码已发送，5分钟内有效');
                        startCountdown(btn);
                    } else {
                        showAlert('error', res.msg || '验证码发送失败');
                        btn.disabled = false;
                        btn.textContent = btn.dataset.originalText || '获取验证码';
                    }
                });
            });
        });
    }

    function startCountdown(btn) {
        var countdown = parseInt(btn.dataset.countdown || '60', 10);
        var originalText = btn.dataset.originalText || '获取验证码';
        btn.textContent = countdown + 's 后重发';

        var timer = setInterval(function () {
            countdown--;
            if (countdown <= 0) {
                clearInterval(timer);
                btn.disabled = false;
                btn.textContent = originalText;
            } else {
                btn.textContent = countdown + 's 后重发';
            }
        }, 1000);
    }

    /* ========== 密码强度检测 ========== */
    function initPasswordStrength() {
        document.querySelectorAll('[data-strength]').forEach(function (input) {
            var strengthEl = document.querySelector(input.dataset.strength);
            if (!strengthEl) return;

            input.addEventListener('input', function () {
                var value = input.value;
                var segments = strengthEl.querySelectorAll('.strength-segment');
                var textEl = strengthEl.querySelector('.strength-text');

                if (!value) {
                    strengthEl.classList.remove('visible');
                    return;
                }

                strengthEl.classList.add('visible');

                var score = 0;
                if (value.length >= 6) score++;
                if (value.length >= 10) score++;
                if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score++;
                if (/\d/.test(value)) score++;
                if (/[^a-zA-Z0-9]/.test(value)) score++;

                var level = 'weak', levelText = '弱', activeCount = 1;
                if (score >= 4) { level = 'strong'; levelText = '强'; activeCount = 3; }
                else if (score >= 2) { level = 'medium'; levelText = '中'; activeCount = 2; }

                segments.forEach(function (seg, i) {
                    seg.classList.remove('active', 'weak', 'medium', 'strong');
                    if (i < activeCount) seg.classList.add('active', level);
                });

                textEl.textContent = '密码强度：' + levelText;
            });
        });
    }

    /* ========== 步骤切换 ========== */
    function initSteps() {
        var stepPanels = document.querySelectorAll('.step-panel');
        var stepItems = document.querySelectorAll('.steps .step');
        if (stepPanels.length === 0) return;

        function goToStep(stepIndex) {
            stepPanels.forEach(function (p, i) { p.classList.toggle('active', i === stepIndex); });
            stepItems.forEach(function (s, i) {
                s.classList.remove('active', 'completed');
                if (i < stepIndex) s.classList.add('completed');
                if (i === stepIndex) s.classList.add('active');
            });
        }

        document.querySelectorAll('[data-next-step]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                goToStep(parseInt(btn.dataset.nextStep, 10));
            });
        });

        document.querySelectorAll('[data-prev-step]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                goToStep(parseInt(btn.dataset.prevStep, 10));
            });
        });

        window.goToStep = goToStep;
    }

    /* ========== 实时验证 ========== */
    function initLiveValidation() {
        document.querySelectorAll('.form-input').forEach(function (input) {
            input.addEventListener('input', function () {
                var group = input.closest('.form-group');
                if (group && group.classList.contains('has-error')) clearError(group);
            });
        });
    }

    /* ========== 提示消息 ========== */
    function showAlert(type, message) {
        var alertEl = document.querySelector('.alert');
        if (!alertEl) return;
        alertEl.className = 'alert alert-' + type + ' show';
        var textEl = alertEl.querySelector('.alert-text');
        if (textEl) textEl.textContent = message;
        setTimeout(function () { alertEl.classList.remove('show'); }, 5000);
    }

    /* ========== 按钮加载状态 ========== */
    function setButtonLoading(btn, loading, originalText) {
        if (loading) {
            btn.classList.add('loading');
            btn.disabled = true;
            var btnText = btn.querySelector('.btn-text');
            if (btnText) btnText.innerHTML = '<span class="spinner"></span> 处理中...';
        } else {
            btn.classList.remove('loading');
            btn.disabled = false;
            var btnText = btn.querySelector('.btn-text');
            if (btnText && originalText) btnText.textContent = originalText;
        }
    }

    /* ========== 初始化 ========== */
    document.addEventListener('DOMContentLoaded', function () {
        initParticles();
        initPasswordToggle();
        initCaptchaCountdown();
        initPasswordStrength();
        initSteps();
        initLiveValidation();
    });

    // 暴露工具方法
    window.AuthUtils = {
        validateField: validateField,
        showAlert: showAlert,
        showError: showError,
        clearError: clearError,
        setButtonLoading: setButtonLoading,
        Validators: Validators
    };
})();
