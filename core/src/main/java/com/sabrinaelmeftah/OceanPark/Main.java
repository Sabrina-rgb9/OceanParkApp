package com.sabrinaelmeftah.OceanPark;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

import com.sabrinaelmeftah.OceanPark.screens.*;
import com.sabrinaelmeftah.OceanPark.gametools.LevelLoader;

public class Main extends Game {
    public SpriteBatch batch;
    public Skin skin;
    public WebSocketClient socket;
    private final Array<String> messageQueue = new Array<>();

    public String playerId;
    public String playerName;

    // Arrays de frames para cada estado
    public TextureRegion[] framesIdle;
    public TextureRegion[] framesLeft;
    public TextureRegion[] framesRight;

    public Texture tilesetTexture;
    public int[][] tileMap;
    public LevelLoader levelLoader;

    public TextureRegion[] leafKeyFrames;

    @Override
    public void create() {
        batch = new SpriteBatch();
        skin = new Skin(Gdx.files.internal("skin/neon-ui.json"));

        // Carga y corte de animaciones (32x32 px)
        framesIdle = cargarAnimacion("sprites/Mushroom Idle.png", 32, 32);
        framesLeft = cargarAnimacion("sprites/Mushroom Left.png", 32, 32);
        framesRight = cargarAnimacion("sprites/Mushroom Right.png", 32, 32);

        // Cargar Mapa
        tilesetTexture = new Texture(Gdx.files.internal("map/tileset_cueva_2.png"));
        levelLoader = new LevelLoader();
        levelLoader.load();
        tileMap = levelLoader.tileMap;

        Texture keyTex = new Texture(Gdx.files.internal("sprites/Leaf Key.png"));
        keyTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Si la imagen mide 64x32 (2 frames de 32x32), lo cortamos así:
        TextureRegion[][] keyTmp = TextureRegion.split(keyTex, 32, 32);
        leafKeyFrames = new TextureRegion[]{ keyTmp[0][0], keyTmp[0][1] };

        conectar();
        this.setScreen(new MainMenu(this));
    }

    // Función auxiliar para no repetir código y evitar el "filtro borroso"
    private TextureRegion[] cargarAnimacion(String path, int width, int height) {
        Texture tex = new Texture(Gdx.files.internal(path));
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] tmp = TextureRegion.split(tex, width, height);

        // Convertimos la matriz en un array simple (asumiendo que están en la fila 0)
        TextureRegion[] frames = new TextureRegion[tmp[0].length];
        System.arraycopy(tmp[0], 0, frames, 0, tmp[0].length);
        return frames;
    }

    private void conectar() {
        try {
            socket = new WebSocketClient(new URI("ws://localhost:3000")) {
                @Override public void onOpen(ServerHandshake h) { Gdx.app.log("WS", "Abierto"); }
                @Override public void onMessage(String m) { synchronized(messageQueue){ messageQueue.add(m); } }
                @Override public void onClose(int c, String r, boolean rem) {}
                @Override public void onError(Exception e) {}
            };
            socket.connect();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void render() {
        synchronized(messageQueue) {
            for (String m : messageQueue) {
                if (getScreen() instanceof IScreen) ((IScreen)getScreen()).handleMessage(m);
            }
            messageQueue.clear();
        }
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        if(skin != null) skin.dispose();
        if(tilesetTexture != null) tilesetTexture.dispose();
        // Nota: En un proyecto real, deberías hacer dispose de las texturas de los frames también
    }
}
