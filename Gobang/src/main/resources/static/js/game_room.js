// 初始化信息对象
let gameInfo = {
    roomId: null,
    thisUserId: null,
    thatUserId: null,
    isWhite: true
}

// 设定界面显示相关操作
function setScreenText(me) {
    let screen = document.querySelector('#screen');
    if (me) {
        screen.innerHTML = "轮到你落子了!";
    } else {
        screen.innerHTML = "轮到对方落子了!";
    }
}


// 初始化 websocket(此处的路径写作 /game, 不要写作 /game/ )
let websocketUrl = "ws://" + location.host + "/game";
let websocket = new WebSocket(websocketUrl);
websocket.onopen = function () {
    console.log("连接有房间成功!");
}

websocket.close = function () {
    console.log("游戏服务器断开连接!");
}

websocket.onerror = function () {
    console.log("游戏服务器的连接出现异常!")
}

websocket.onbeforeunload = function () {
    websocket.close();
}

// 处理服务器建立连接返回的数据响应
websocket.onmessage = function (event) {
    console.log("[handlerGameReady] " + event.data);
    let resp = JSON.parse(event.data);
    console.log(resp);
    if (!resp.ok) {
        alert("连接游戏失败! reason: " + resp.reason);
        // 连接游戏失败, 返回游戏大厅
        location.assign("/game_hall.html");
        return;
    }

    if (resp.message == 'gameReady') {
        // 得到的是正确的响应, 响应数据赋值给 gameInfo 对象
        gameInfo.roomId = resp.roomId;
        gameInfo.thatUserId = resp.thatUserId;
        gameInfo.thisUserId = resp.thisUserId;
        gameInfo.isWhite = (resp.whiteUser == resp.thisUserId);

        // 初始化棋盘
        initGame();

        // 设置显示区域的内容
        setScreenText(gameInfo.isWhite);

    } else if (resp.message == 'repeatConnection') {
        alert("检测到游戏多开! 请使用其他账号登录!");
        location.assign("/login.html");
    } else {
        console.log("响应类型错误!");
        return;
    }
}

// 初始化一局游戏
function initGame() {
    // 是我下还是对方下. 根据服务器分配的先后手情况决定
    let me = gameInfo.isWhite;
    // 游戏是否结束
    let over = false;
    let chessBoard = [];
    //初始化chessBord数组(表示棋盘的数组)
    for (let i = 0; i < 15; i++) {
        chessBoard[i] = [];
        for (let j = 0; j < 15; j++) {
            chessBoard[i][j] = 0;
        }
    }
    let chess = document.querySelector('#chess');
    let context = chess.getContext('2d');
    context.strokeStyle = "#BFBFBF";
    // 背景图片
    let logo = new Image();
    logo.src = "image/sky.jpeg";
    logo.onload = function () {
        context.drawImage(logo, 0, 0, 450, 450);
        initChessBoard();
    }

    // 绘制棋盘网格
    function initChessBoard() {
        for (let i = 0; i < 15; i++) {
            context.moveTo(15 + i * 30, 15);
            context.lineTo(15 + i * 30, 430);
            context.stroke();
            context.moveTo(15, 15 + i * 30);
            context.lineTo(435, 15 + i * 30);
            context.stroke();
        }

        /**
         * 之前 websocket.onmessage 主要是用来处理了游戏就绪响应,
         * 在游戏就绪之后, 初始化完毕之后, 也就不再有这个游戏就绪响应了.
         * 所以, 就在这个 initGame方法 内部, 修改 websocket.onmessage 方法~
         * 让这个方法里面针对落子响应进行处理!
         */
        websocket.onmessage = function (event) {
            let resp = JSON.parse(event.data);
            if (resp.message != 'putChess') {
                console.log("not putChess resp!")
                return;
            }

            // 先判定当前这个响应是自己落的子, 还是对方落的子
            if (resp.userId == gameInfo.thisUserId) {
                // 自己落子: 根据我自己棋子的颜色, 来绘制一个棋子
                oneStep(resp.col, resp.row, gameInfo.isWhite);
            } else if (resp.userId == gameInfo.thatUserId) {
                // 对手落子: 根据我对手棋子的颜色, 来绘制一个棋子
                oneStep(resp.col, resp.row, !gameInfo.isWhite);
            } else {
                // 响应的 userId 错误!
                console.log('[handlerPutChess] 响应的 userId 错误!');
                return;
            }

            // 给对应的位置设为 1, 方便后续逻辑判定当前位置是否已经有子了
            chessBoard[resp.row][resp.col] = 1;

            // 交换双方的落子轮次
            me = !me;
            setScreenText(me);

            // 判定游戏是否结束
            let screenDiv = document.querySelector('#screen');
            if (resp.winner != 0) {
                console.log("winner",resp.winner)
                if (resp.winner == gameInfo.thisUserId) {
                    screenDiv.innerHTML = '你赢了!'
                } else if (resp.winner == gameInfo.thatUserId) {
                    screenDiv.innerHTML = '你输了!'

                } else {
                    alert("[winner]字段错误: " + resp.winner)
                }

                // 增加一个按钮, 点击之后回到游戏大厅
                let backButton = document.createElement('button');
                backButton.innerHTML = '回到大厅';
                let screen = document.querySelector('#screen');
                backButton.onclick = function () {
                    screen.innerHTML = '等待5s返回';
                    setTimeout(function(){
                        location.replace('/game_hall.html');
                    },5000)
                }
                let fatherDiv = document.querySelector('.container>div');
                fatherDiv.appendChild(backButton);
            }
        }
    }

    // 绘制一个棋子, me 为 true
    function oneStep(i, j, isWhite) {
        context.beginPath();
        context.arc(15 + i * 30, 15 + j * 30, 13, 0, 2 * Math.PI);
        context.closePath();
        var gradient = context.createRadialGradient(15 + i * 30 + 2, 15 + j * 30 - 2, 13, 15 + i * 30 + 2, 15 + j * 30 - 2, 0);
        if (!isWhite) {
            gradient.addColorStop(0, "#0A0A0A");
            gradient.addColorStop(1, "#636766");
        } else {
            gradient.addColorStop(0, "#D1D1D1");
            gradient.addColorStop(1, "#F9F9F9");
        }
        context.fillStyle = gradient;
        context.fill();
    }

    chess.onclick = function (e) {
        if (over) {
            return;
        }
        if (!me) {
            return;
        }
        let x = e.offsetX;
        let y = e.offsetY;
        // 注意, 横坐标是列, 纵坐标是行
        let col = Math.floor(x / 30);
        let row = Math.floor(y / 30);
        if (chessBoard[row][col] == 0) {
            // 发送坐标给服务器, 服务器要返回结果
            sendRowAndCol(row, col);
        }
    }

    function sendRowAndCol(row, col) {
        let request = {
            message: 'putChess',
            userId: gameInfo.thisUserId,
            row: row,
            col: col
        };

        websocket.send(JSON.stringify(request));
    }
}
