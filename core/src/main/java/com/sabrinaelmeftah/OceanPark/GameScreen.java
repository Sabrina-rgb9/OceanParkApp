package com.sabrinaelmeftah.OceanPark;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameScreen implements Screen {
    private final Main game;
    private OrthographicCamera camera;
    private FitViewport viewport;

    private TextureRegion tilePiso;
    private TextureRegion[] framesIdle, framesLeft, framesRight;
    private Texture keyTexture;

    private JsonValue ultimoEstado;
    private float tiempoAnimacion = 0;

    public GameScreen(Main game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(320, 180, camera);

        // 1. RECORTE DEL MAPA (Tileset de 23x23)
        Texture tileset = new Texture(Gdx.files.internal("map/tileset.png"));
        // Extraemos el primer tile de 23x23 píxeles
        tilePiso = new TextureRegion(tileset, 0, 0, 23, 23);

        // 2. RECORTE DE PERSONAJES (Spritesheets de 1 fila x 3 columnas, 32x32 cada uno)
        // Usamos una función auxiliar para no repetir código
        framesIdle = prepararFrames("sprites/Mushroom Idle.png");
        framesLeft = prepararFrames("sprites/Mushroom Left.png");
        framesRight = prepararFrames("sprites/Mushroom Right.png");

        keyTexture = new Texture(Gdx.files.internal("sprites/Leaf Key.png"));
    }

    private TextureRegion[] prepararFrames(String ruta) {
        Texture sheet = new Texture(Gdx.files.internal(ruta));
        // Dividimos la imagen en trozos de 32x32
        TextureRegion[][] temp = TextureRegion.split(sheet, 32, 32);

        // Devolvemos la primera fila completa
        return temp[0];
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.337f, 0.545f, 0.694f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (ultimoEstado == null) return;

        tiempoAnimacion += delta;
        procesarEntrada();
        actualizarCamara();
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // DIBUJAR PLATAFORMAS (23x23 píxeles según el servidor)
        JsonValue world = ultimoEstado.get("world");
        for (JsonValue plat : world.get("platforms")) {
            game.batch.draw(tilePiso,
                plat.getFloat("x"), plat.getFloat("y"),
                plat.getFloat("width"), plat.getFloat("height"));
        }

        // DIBUJAR LLAVE
        JsonValue key = world.get("key");
        if (!key.getBoolean("taken")) {
            game.batch.draw(keyTexture, key.getFloat("x"), key.getFloat("y"), 24, 24);
        }

        // DIBUJAR JUGADORES
        for (JsonValue p : ultimoEstado.get("players")) {
            float x = p.getFloat("x");
            float y = p.getFloat("y");
            boolean mirandoDerecha = p.getBoolean("facingRight");
            String estado = p.getString("state");

            // SELECCIÓN DE ANIMACIÓN (1 fila x 3 columnas)
            TextureRegion[] animacionActual = framesIdle;
            if (estado.equals("WALKING") || estado.equals("RUNNING")) {
                animacionActual = mirandoDerecha ? framesRight : framesLeft;
            }

            // Elegimos uno de los 3 frames basado en el tiempo (animación simple)
            int frameIndex = (int)(tiempoAnimacion * 10) % animacionActual.length;
            TextureRegion frameParaDibujar = animacionActual[frameIndex];

            game.batch.draw(frameParaDibujar, x, y, 32, 32);
        }

        game.batch.end();
    }

    private void actualizarCamara() {
        for (JsonValue p : ultimoEstado.get("players")) {
            if (p.getString("id").equals(game.playerId)) {
                camera.position.set(p.getFloat("x"), p.getFloat("y"), 0);
                break;
            }
        }
    }

    private void procesarEntrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) game.socket.send("{\"type\":\"JOIN\"}"); // O tu comando de salto
        if (Gdx.input.isKeyPressed(Input.Keys.A)) game.socket.send("{\"type\":\"INPUT\",\"input\":\"LEFT\"}");
        if (Gdx.input.isKeyPressed(Input.Keys.D)) game.socket.send("{\"type\":\"INPUT\",\"input\":\"RIGHT\"}");
    }

    public void msg(String msg) {
        try {
            JsonValue base = game.lector.parse(msg);
            if (base.getString("type").equals("STATE")) {
                this.ultimoEstado = base;
            }
        } catch (Exception e) { }
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() { /* Añadir todos los disposes aquí */ }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
