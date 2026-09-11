#!/bin/bash
# Tiamo AI 全栈系统一键启动脚本
# 适用于 GitHub Codespaces 环境

echo "=========================================="
echo "  Tiamo AI 全栈系统启动中..."
echo "=========================================="

# 切换到项目根目录
cd /workspaces/tiamo-fullstack 2>/dev/null || cd "$(dirname "$0")"

# 启动Redis
echo "📦 启动Redis服务..."
redis-server --daemonize yes 2>/dev/null || sudo redis-server --daemonize yes 2>/dev/null || echo "⚠️  Redis启动失败，继续尝试..."
sleep 2
if redis-cli ping 2>/dev/null | grep -q PONG; then
    echo "✅ Redis已启动"
else
    echo "⚠️  Redis可能未启动，后端将使用无缓存模式"
fi

# 检查后端jar包是否存在
if [ ! -f "tiamo-backend/target/tiamo-backend-1.0.0.jar" ]; then
    echo "📦 后端未编译，正在编译（约1-2分钟）..."
    cd tiamo-backend
    mvn clean package -DskipTests 2>&1 | tail -20
    if [ ! -f "target/tiamo-backend-1.0.0.jar" ]; then
        echo "❌ 后端编译失败！请检查错误信息"
        exit 1
    fi
    cd ..
    echo "✅ 后端编译完成"
fi

# 检查后端是否已在运行
if pgrep -f "tiamo-backend-1.0.0.jar" > /dev/null; then
    echo "ℹ️  后端服务已在运行"
else
    # 启动后端（端口8080）
    echo "🚀 启动后端服务（端口8080）..."
    cd tiamo-backend
    nohup java -jar target/tiamo-backend-1.0.0.jar --server.port=8080 > /tmp/backend.log 2>&1 &
    BACKEND_PID=$!
    cd ..
    echo "✅ 后端已启动，PID: $BACKEND_PID"
    
    # 等待后端启动（最多60秒）
    echo "⏳ 等待后端启动（最多60秒）..."
    for i in $(seq 1 30); do
        sleep 2
        if curl -s http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"test","password":"test"}' 2>/dev/null | grep -q "code"; then
            echo "✅ 后端启动成功！"
            break
        fi
        if [ $i -eq 30 ]; then
            echo "⚠️  后端启动超时，查看日志: tail -50 /tmp/backend.log"
        fi
    done
fi

# 检查前端是否已在运行
if pgrep -f "http.server 8083" > /dev/null; then
    echo "ℹ️  前端服务已在运行"
else
    # 启动前端（端口8083）
    echo "🌐 启动前端服务（端口8083）..."
    cd /workspaces/tiamo-fullstack/tiamo-auth
    nohup python3 -m http.server 8083 --bind 0.0.0.0 > /tmp/frontend.log 2>&1 &
    cd /workspaces/tiamo-fullstack
    sleep 2
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/login.html | grep -q "200"; then
        echo "✅ 前端已启动"
    else
        echo "⚠️  前端启动可能失败，查看日志: cat /tmp/frontend.log"
    fi
fi

echo ""
echo "=========================================="
echo "  🎉 系统启动完成！"
echo "=========================================="
echo ""
echo "  前端地址: http://localhost:8083"
echo "  后端API:  http://localhost:8080"
echo ""
echo "  Codespaces端口转发提示："
echo "  - 在端口面板中设置8080和8083为'公开'"
echo "  - 首次访问需要GitHub认证"
echo ""
echo "  查看后端日志: tail -f /tmp/backend.log"
echo "  查看前端日志: tail -f /tmp/frontend.log"
echo ""
