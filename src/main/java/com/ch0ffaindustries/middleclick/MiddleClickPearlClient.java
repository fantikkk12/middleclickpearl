package com.ch0ffaindustries.middleclick;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/**
 * Порт middleClickPerl (Forge 1.16.5) на Fabric 1.21.4.
 * Средняя кнопка мыши -> бросок эндер-жемчуга из хотбара или инвентаря.
 */
public class MiddleClickPearlClient implements ClientModInitializer {

	private boolean wasPressed = false;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(MinecraftClient client) {
		if (client.getWindow() == null) return;

		boolean pressed = GLFW.glfwGetMouseButton(
				client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

		// реагируем только на момент нажатия (как action == PRESS в оригинале)
		boolean justPressed = pressed && !wasPressed;
		wasPressed = pressed;

		if (!justPressed) return;
		if (client.currentScreen != null) return;
		if (client.player == null || client.world == null || client.interactionManager == null) return;

		throwPearl(client, client.player);
	}

	private void throwPearl(MinecraftClient client, ClientPlayerEntity player) {
		PlayerInventory inv = player.getInventory();
		int slot = findEnderPearl(inv);
		if (slot == -1) return;

		if (slot < 9) {
			// жемчуг в хотбаре: переключаемся, бросаем, возвращаемся
			int prev = inv.selectedSlot;
			inv.selectedSlot = slot;
			client.interactionManager.interactItem(player, Hand.MAIN_HAND);
			inv.selectedSlot = prev;
		} else {
			// жемчуг в основном инвентаре: меняем местами с текущим слотом хотбара,
			// бросаем, меняем обратно
			int hotbar = inv.selectedSlot;
			int syncId = player.playerScreenHandler.syncId;
			// в playerScreenHandler слоты 9..35 = основной инвентарь (совпадает с индексами PlayerInventory)
			client.interactionManager.clickSlot(syncId, slot, hotbar, SlotActionType.SWAP, player);
			client.interactionManager.interactItem(player, Hand.MAIN_HAND);
			client.interactionManager.clickSlot(syncId, slot, hotbar, SlotActionType.SWAP, player);
		}
	}

	/** Хотбар (0-8) в приоритете, затем основной инвентарь (9-35). */
	private int findEnderPearl(PlayerInventory inv) {
		for (int i = 0; i < 36; i++) {
			if (inv.getStack(i).isOf(Items.ENDER_PEARL)) return i;
		}
		return -1;
	}
}
