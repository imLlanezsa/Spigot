package io.github.cspigot.utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class Clickable {

	private final List<TextComponent> components = new ArrayList<>();
	private String hoverText;
	private String text = "";

	public Clickable(String msg) {
		TextComponent message = new TextComponent(TextComponent.fromLegacyText(CC.translate(msg)));

		this.components.add(message);
		this.text = msg;
	}

	public Clickable(String msg, String hoverMsg, String clickString) {
		this.add(msg, hoverMsg, clickString);
		this.text = msg;
		this.hoverText = hoverMsg;
	}

	public Clickable(String msg, String hoverMsg, String clickString, boolean suggest) {
		this.add(CC.translate(msg), CC.translate(hoverMsg), clickString, suggest);
		this.text = msg;
		this.hoverText = hoverMsg;
	}

	public TextComponent add(String msg, String hoverMsg, String clickString) {
		return this.add(msg, hoverMsg, clickString, false);
	}

	public TextComponent add(String msg, String hoverMsg, String clickString, boolean suggest) {
		TextComponent message = new TextComponent(CC.translate(msg));

		if (hoverMsg != null) {
			message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(CC.translate(hoverMsg)).create()));
		}

		if (clickString != null) {
			message.setClickEvent(new ClickEvent(suggest ? ClickEvent.Action.SUGGEST_COMMAND : ClickEvent.Action.RUN_COMMAND, clickString));
		}

		this.components.add(message);
		this.text = msg;
		this.hoverText = hoverMsg;

		return message;
	}

	public void add(String message) {
		this.text = message;
		this.components.add(new TextComponent(CC.translate(message)));
	}

	public void sendToPlayer(Player player) {
		player.sendMessage(this.asComponents());
	}

	public TextComponent[] asComponents() {
		return this.components.toArray(new TextComponent[0]);
	}
}
