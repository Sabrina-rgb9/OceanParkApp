package com.sabrinaelmeftah.OceanPark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;

import com.sabrinaelmeftah.OceanPark.Main;
import com.sabrinaelmeftah.OceanPark.gametools.LevelRenderer;

public class MainMenu implements Screen, IScreen {

    private final Main game;

    private Stage stage;
    private OrthographicCamera mapCamera;
    private FitViewport mapViewport;

    private Label labelInfo;
    private Label labelPlayers;

    private final JsonReader lector = new JsonReader();

    private final float MAP_X = -295.0f;
    private final float MAP_Y = 673.0f;

    public MainMenu(final Main game) {
        this.game = game;

        mapCamera = new OrthographicCamera();
        mapViewport = new FitViewport(480, 270, mapCamera);
        mapCamera.position.set(240, 135, 0);
        mapCamera.zoom = 1.07f;
        mapCamera.update();

        stage = new Stage(new FitViewport(1280, 720), game.batch);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(40);
        stage.addActor(root);

        Table menuTable = new Table();
        Table playersTable = new Table();

        Label title = new Label("OCEAN PARK", game.skin);
        final TextField nombreInput = new TextField("", game.skin);
        TextButton btnJugar = new TextButton("Entrar", game.skin);

        labelInfo = new Label("", game.skin);
        labelPlayers = new Label("Jugadores conectados:\n\nNadie conectado", game.skin);

        menuTable.add(title).padBottom(20).row();
        menuTable.add(nombreInput).width(260).height(50).padBottom(20).row();
        menuTable.add(btnJugar).width(260).height(55).padBottom(20).row();
        menuTable.add(labelInfo);

        playersTable.add(labelPlayers).top().left().pad(20);

        root.add(menuTable).expand().center();
        root.add(playersTable).width(340).height(500).right().top();

        btnJugar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String nombre = nombreInput.getText().trim();

                if (!nombre.isEmpty() && game.socket != null && game.socket.isOpen()) {
                    String nombreSeguro = nombre.replace("\"", "\\\"");
                    String msg = "{\"type\":\"JOIN\", \"name\":\"" + nombreSeguro + "\"}";

                    game.socket.send(msg);
                    labelInfo.setText("Conectando con el servidor...");

                } else if (game.socket == null || !game.socket.isOpen()) {
                    labelInfo.setText("Error: Sin conexión al servidor.");
                } else {
                    labelInfo.setText("Introduce un nombre.");
                }
            }
        });
    }

    @Override
    public void handleMessage(String message) {
        try {
            JsonValue json = lector.parse(message);
            String type = json.getString("type", "");

            if (type.equals("JOINED")) {
                game.playerId = json.getString("playerId");
                game.playerName = json.getString("name");

                Gdx.app.postRunnable(() -> game.setScreen(new GameScreen(game)));
            }

            if (type.equals("STATE")) {
                actualizarListaJugadores(json.get("players"));
            }

            if (type.equals("ERROR")) {
                labelInfo.setText(json.getString("message", "Error desconocido"));
            }

        } catch (Exception e) {
            Gdx.app.log("MainMenu", "Mensaje ignorado o error: " + e.getMessage());
        }
    }

    private void actualizarListaJugadores(JsonValue players) {
        if (players == null || players.size == 0) {
            labelPlayers.setText("Jugadores conectados:\n\nNadie conectado");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Jugadores conectados:\n\n");

        for (JsonValue p : players) {
            sb.append("- ")
                .append(p.getString("name", "Jugador"))
                .append("\n");
        }

        labelPlayers.setText(sb.toString());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        dibujarMapaFondo();

        stage.act(delta);
        stage.draw();
    }

    private void dibujarMapaFondo() {
        if (game.tileMap == null || game.tilesetTexture == null) return;

        mapViewport.apply();
        game.batch.setProjectionMatrix(mapCamera.combined);

        game.batch.begin();

        LevelRenderer.render(
            game.batch,
            game.tilesetTexture,
            game.tileMap,
            MAP_X,
            MAP_Y
        );

        game.batch.end();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        mapViewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
