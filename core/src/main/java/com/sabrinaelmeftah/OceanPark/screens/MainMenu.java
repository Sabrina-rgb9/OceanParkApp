package com.sabrinaelmeftah.OceanPark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
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

public class MainMenu implements Screen, IScreen {
    private final Main game;
    private Stage stage;
    private Label labelInfo;
    private final JsonReader lector = new JsonReader();

    public MainMenu(final Main game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(1280, 720), game.batch);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("OCEAN PARK", game.skin);
        final TextField nombreInput = new TextField("", game.skin);
        TextButton btnJugar = new TextButton("Entrar", game.skin);
        labelInfo = new Label("", game.skin);

        table.add(title).padBottom(20).row();
        table.add(nombreInput).width(200).padBottom(20).row();
        table.add(btnJugar).width(200).padBottom(20).row();
        table.add(labelInfo);

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
                }
            }
        });
    }

    @Override
    public void handleMessage(String message) {
        try {
            JsonValue json = lector.parse(message);

            if (json.getString("type").equals("JOINED")) {
                game.playerId = json.getString("playerId");
                game.playerName = json.getString("name");

                Gdx.app.postRunnable(() -> game.setScreen(new GameScreen(game)));
            }

            if (json.getString("type").equals("ERROR")) {
                labelInfo.setText(json.getString("message", "Error desconocido"));
            }

        } catch (Exception e) {
            Gdx.app.log("MainMenu", "Mensaje ignorado o error: " + e.getMessage());
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
