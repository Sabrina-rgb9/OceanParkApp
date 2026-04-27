package com.sabrinaelmeftah.OceanPark;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MenuScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private Label labelInfo;

    public MenuScreen(final Main game) {
        this.game = game;
        // Stage en HD (1280x720) para que el estilo Neon se vea nítido y no pixelado como en tu captura
        this.stage = new Stage(new FitViewport(1280, 720), game.batch);

        // 1. Cargar el Skin Neon (Asegúrate de que el nombre del archivo sea exacto)
        this.skin = new Skin(Gdx.files.internal("skin/neon-ui.json"));

        // 2. Crear elementos usando los estilos definidos en el JSON
        // Usamos el estilo por defecto para el título
        Label titulo = new Label("OCEAN PARK", skin);
        titulo.setFontScale(2.5f);

        // Usamos el estilo "login" para el cuadro de texto
        final TextField nombreInput = new TextField("", skin, "login");
        nombreInput.setMessageText("NOMBRE DEL JUGADOR");

        // Botón de conectar
        TextButton btnLogin = new TextButton("CONECTAR", skin);

        // Label para mensajes de estado
        labelInfo = new Label("", skin);

        // 3. Lógica del botón de Login
        btnLogin.addListener(new ClickListener() {
            // Dentro del ClickListener del botón en MenuScreen.java
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String nombre = nombreInput.getText().trim();
                if (!nombre.isEmpty()) {
                    // CAMBIO CRÍTICO: El servidor espera "JOIN", no "LOGIN"
                    String jsonJoin = "{\"type\":\"JOIN\",\"name\":\"" + nombre + "\"}";

                    Gdx.app.log("WS_SEND", "Enviando: " + jsonJoin);

                    if (game.socket != null && game.socket.isOpen()) {
                        game.socket.send(jsonJoin);
                        labelInfo.setText("Entrando en la sala...");
                    } else {
                        labelInfo.setText("Sin conexión");
                    }
                }
            }
        });

        // 4. Organización visual con Table
        Table table = new Table();
        table.setFillParent(true);

        table.add(titulo).padBottom(60).row();
        table.add(nombreInput).size(500, 80).padBottom(20).row();
        table.add(btnLogin).size(350, 100).padBottom(20).row();
        table.add(labelInfo);

        stage.addActor(table);
    }

    // Método para recibir mensajes del servidor
    public void msg(String msg) {
        try {
            JsonValue base = game.lector.parse(msg);
            String tipo = base.getString("type");

            if (tipo.equals("JOINED")) {
                // Guardamos los datos que envía tu servidor Node.js/JS
                game.playerId = base.getString("playerId");
                game.playerName = base.getString("name");

                // IMPORTANTE: Saltamos al juego
                Gdx.app.postRunnable(() -> game.setScreen(new GameScreen(game)));

                // Aquí ya puedes saltar al juego
                // game.setScreen(new GameScreen(game));

            } else if (tipo.equals("ERROR")) {
                labelInfo.setText(base.getString("message"));
            }
        } catch (Exception e) {
            Gdx.app.error("JSON_ERROR", "Error al leer respuesta: " + msg);
        }
    }

    @Override
    public void render(float delta) {
        // Fondo oscuro para que resalte el Neon
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
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
