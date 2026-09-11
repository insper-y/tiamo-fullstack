#!/bin/bash
# Tiamo AI 全栈系统一键启动脚本
# 适用于 GitHub Codespaces 环境

echo "=========================================="
echo "  Tiamo AI 全栈系统启动中..."
echo "=========================================="

# 检查后端jar包是否存在
if [ ! -f "tiamo-backend/target/tiamo-backend-1.0.0.jar" ]; then
    echo "📦 后端未编译，正在编译..."
    cd tiamo-backend
    mvn clean package -DskipTests -q
    cd ..
    echo "✅ 后端编译完成"
fi

# 启动后端（端口8080）
echo "🚀 启动后端服务（端口8080）..."
cd tiamo-backend
nohup java -jar target/tiamo-backend-1.0.0.jar --server.port=8080 > /tmp/backend.log 2>&1 &
BACKEND_PID=$!
cd ..
echo "✅ 后端已启动 (PID: $BACKEND_PID)"

# 启动前端（端口8083）
echo "🌐 启动前端服务（端口8083）..."
cd tiamo-auth
nohup python3 -m http.server 8083 > /tmp/frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..
echo "✅ 前端已启动 (PID: $FRONTEND_PID)"

# 等待服务启动
echo ""
echo "⏳ 等待服务启动（约10秒）..."
sleep 10

# 检查后端状态
if curl -s http://localhost:8080/api/server-status > /dev/null 2>&1; then
    echo "✅ 后端服务运行正常"
else
    echo "⚠️  后端可能还在启动中，请稍后刷新页面"
fi

echo ""
echo "=========================================="
echo "  🎉 系统启动完成！"
echo "=========================================="
echo ""
echo "  前端地址: http://localhost:8083"
echo "  后端API:  http://localhost:8080"
echo ""
echo "  Codespaces会自动弹出端口转发提示，"
echo "  点击 'Open in Browser' 即可访问前端。"
echo ""
echo "  查看后端日志: tail -f /tmp/backend.log"
echo "  查看前端日志: tail -f /tmp/frontend.log"
echo "  停止服务: kill $BACKEND_PID $FRONTEND_PID"
echo ""
