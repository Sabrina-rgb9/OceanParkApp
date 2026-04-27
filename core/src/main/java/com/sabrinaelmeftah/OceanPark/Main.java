package com.sabrinaelmeftah.OceanPark;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.badlogic.gdx.Screen;
import com.sabrinaelmeftah.OceanPark.MenuScreen;
import com.sabrinaelmeftah.OceanPark.GameScreen;

import java.net.URI;

public class Main extends Game {
    public SpriteBatch batch;
    public BitmapFont font;
    public WebSocketClient socket;
    public JsonReader lector;

    // Datos del jugador que persistirán entre pantallas
    public String playerId;
    public String playerName;

    // Cola de mensajes para procesar en el hilo principal de renderizado
    public final Array<String> queue = new Array<>();

    @Override
    public void create() {
        batch = new SpriteBatch();
        lector = new JsonReader();

        // 1. CARGA DE FUENTE PIXEL (Para el título o textos personalizados)
        // Asegúrate de que el archivo existe en assets/fonts/pixel.ttf
        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/pixel.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 24;
            font = generator.generateFont(parameter);
            generator.dispose();
        } catch (Exception e) {
            Gdx.app.error("ERROR", "No se encontró la fuente en assets/fonts/pixel.ttf. Usando fuente por defecto.");
            font = new BitmapFont(); // Fallback por si no encuentra el archivo
        }

        // 2. CONEXIÓN AL SERVIDOR (WebSocket)
        conectarServidor();

        // 3. INICIAR EN EL MENÚ
        this.setScreen(new MenuScreen(this));
    }

    private void conectarServidor() {
        try {
            // Sustituye por la IP y puerto de tu servidor
            URI uri = new URI("wss://pico3.ieti.site");
            socket = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Gdx.app.log("NET", "Conectado a Pico3 con éxito");
                }

                // Dentro de tu WebSocketClient en Main.java
                @Override
                public void onMessage(String message) {
                    // Añade este log para confirmar si el cable "trae datos"
                    Gdx.app.log("WS_RAW", "Recibido del servidor: " + message);

                    synchronized (queue) {
                        queue.add(message);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Gdx.app.log("SOCKET", "Conexión cerrada: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    Gdx.app.error("SOCKET", "Error en el socket: " + ex.getMessage());
                }
            };
            socket.connect();
        } catch (Exception e) {
            Gdx.app.error("NET", "Error al conectar con pico3: " + e.getMessage());
        }
    }

    @Override
    public void render() {
        synchronized (queue) {
            if (queue.size > 0) {
                Screen current = getScreen(); // Obtenemos la pantalla actual
                for (String msg : queue) {
                    if (current instanceof MenuScreen) {
                        // Casteamos a MenuScreen para acceder a su método msg
                        ((MenuScreen) current).msg(msg);
                    } else if (current instanceof GameScreen) {
                        // Casteamos a GameScreen para acceder a su método msg
                        ((GameScreen) current).msg(msg);
                    }
                }
                queue.clear();
            }
        }
        super.render();
    }
    @Override
    public void dispose() {
        batch.dispose();
        if (font != null) font.dispose();
        if (socket != null) socket.close();
    }
}
