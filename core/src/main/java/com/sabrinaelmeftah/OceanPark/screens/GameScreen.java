package com.sabrinaelmeftah.OceanPark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabrinaelmeftah.OceanPark.Main;

public class GameScreen implements Screen, IScreen {
    private final Main game;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private JsonValue ultimoEstado;
    private final JsonReader lector = new JsonReader();
    private String lastDir = "NONE";
    private float tiempoAnimacion = 0f;

    // --- COORDENADAS FINALES DE TU CALIBRACIÓN ---
    private final float MAP_X = -295.0f;
    private final float MAP_Y = 673.0f;
    private final float ZOOM = 1.07f;

    // Tus nuevos offsets de personaje
    private final float P_OFF_X = 137.5f;
    private final float P_OFF_Y = 195.5f;

    public GameScreen(Main game) {
        this.game = game;
        camera = new OrthographicCamera();
        // Mantenemos la resolución de 480x270 que funcionó bien
        viewport = new FitViewport(480, 270, camera);
        camera.zoom = ZOOM;
    }

    @Override
    public void render(float delta) {
        // Color de fondo azul cielo original
        Gdx.gl.glClearColor(0.33f, 0.54f, 0.69f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (ultimoEstado == null || game.tileMap == null) return;

        float worldHeight = game.tileMap.length * 23f;
        tiempoAnimacion += delta;

        // 1. SEGUIMIENTO DE CÁMARA (Usando todos los offsets)
        JsonValue players = ultimoEstado.get("players");
        if (players != null) {
            for (JsonValue p : players) {
                if (p.getString("id").equals(game.playerId)) {
                    float tx = p.getFloat("x") + MAP_X + P_OFF_X;
                    float ty = worldHeight - (p.getFloat("y") + MAP_Y + P_OFF_Y) - 16;
                    camera.position.set(tx, ty, 0);
                    camera.update();
                    break;
                }
            }
        }

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // 2. DIBUJAR MAPA
        com.sabrinaelmeftah.OceanPark.gametools.LevelRenderer.render(
            game.batch, game.tilesetTexture, game.tileMap, MAP_X, MAP_Y
        );

        // 2. DIBUJAR LEAF KEY
        JsonValue keyData = ultimoEstado.get("leafKey");
        if (keyData != null) {
            float kx = keyData.getFloat("x") + MAP_X + P_OFF_X;
            // Aplicamos la altura del mundo e inversión Y
            float ky = worldHeight - (keyData.getFloat("y") + MAP_Y + P_OFF_Y) - 32f;

            // Animación entre frame 0 y 1 cada 0.5 segundos
            int frameIdx = (int)(tiempoAnimacion * 4) % 2;
            game.batch.draw(game.leafKeyFrames[frameIdx], kx, ky, 32, 32);
        }

        // 3. DIBUJAR JUGADORES
        if (players != null) {
            for (JsonValue p : players) {
                float px = p.getFloat("x") + MAP_X + P_OFF_X;
                float py = worldHeight - (p.getFloat("y") + MAP_Y + P_OFF_Y) - 32f;

                // 1. ELEGIR EL SET DE ANIMACIÓN
                TextureRegion[] framesActuales;
                String servidorState = p.getString("state", "IDLE"); // RUN o IDLE
                boolean mirandoDerecha = p.getBoolean("facingRight", true);

                if (servidorState.equals("RUN")) {
                    // Si corre, elegimos Left o Right
                    framesActuales = mirandoDerecha ? game.framesRight : game.framesLeft;
                } else {
                    // Si está quieto, usamos Idle
                    framesActuales = game.framesIdle;
                }

                // 2. CALCULAR EL FRAME SEGÚN EL TIEMPO
                // Usamos 8 FPS para que se vea fluido
                int frameIndex = (int)(tiempoAnimacion * 8) % framesActuales.length;
                TextureRegion regionADibujar = framesActuales[frameIndex];

                // 3. DIBUJAR (Solo el trozo de 32x32)
                game.batch.draw(regionADibujar, px, py, 32, 32);

                // Dibujar nombre del jugador
                if (game.skin != null && game.skin.getFont("font") != null) {
                    game.skin.getFont("font").getData().setScale(0.4f);
                    game.skin.getFont("font").draw(game.batch, p.getString("name"), px, py + 45);
                }
            }
        }
        game.batch.end();

        procesarEntrada();
    }

    private void procesarEntrada() {
        // Controles estándar de juego
        String dir = "NONE";
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) dir = "LEFT";
        else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) dir = "RIGHT";

        boolean jump = Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        if (game.socket != null && game.socket.isOpen()) {
            if (!dir.equals(lastDir) || jump) {
                game.socket.send("{\"type\":\"MOVE\", \"dir\":\"" + dir + "\", \"jump\":" + jump + "}");
                lastDir = dir;
            }
        }
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, false); }
    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}

    @Override public void handleMessage(String message) {
        try {
            JsonValue json = lector.parse(message);
            if (json.getString("type").equals("STATE")) this.ultimoEstado = json;
        } catch (Exception e) {}
    }
}
