
@tool
class_name PanelCSS extends Panel

# ============================================================
# ENUMS
# ============================================================

enum ScreenAnchor {
	TOP_LEFT,
	TOP_CENTER,
	TOP_RIGHT,
	CENTER_LEFT,
	CENTER,
	CENTER_RIGHT,
	BOTTOM_LEFT,
	BOTTOM_CENTER,
	BOTTOM_RIGHT
}

enum CSSUnit {
	PX,
	PARENT_PERCENT,
	VW,
	VH,
	AUTO
}

enum PositionUnit {
	PX,
	PARENT_PERCENT,
	VW,
	VH
}

enum BoxSizing {
	CONTENT_BOX,
	BORDER_BOX
}


# ============================================================
# CSS CONFIGURATION
# ============================================================

@export_group("CSS Layout")

@export_subgroup("Transform")

@export var screen_anchor: ScreenAnchor = ScreenAnchor.TOP_LEFT:
	set(value):
		screen_anchor = value
		_request_apply()

@export var css_position := Vector2.ZERO:
	set(value):
		css_position = value
		_request_apply()

@export var position_unit: PositionUnit = PositionUnit.PX:
	set(value):
		position_unit = value
		_request_apply()


@export_subgroup("Dimensions")

@export var css_size := Vector2(100, 100):
	set(value):
		css_size = value
		_request_apply()

@export var width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		width_unit = value
		_request_apply()

@export var height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		height_unit = value
		_request_apply()

@export var css_min_size := Vector2.ZERO:
	set(value):
		css_min_size = value
		_request_apply()

# -1 significa sin límite.
@export var css_max_size := Vector2(-1, -1):
	set(value):
		css_max_size = value
		_request_apply()

@export var min_width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		min_width_unit = value
		_request_apply()

@export var min_height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		min_height_unit = value
		_request_apply()

@export var max_width_unit: CSSUnit = CSSUnit.PX:
	set(value):
		max_width_unit = value
		_request_apply()

@export var max_height_unit: CSSUnit = CSSUnit.PX:
	set(value):
		max_height_unit = value
		_request_apply()

# 0 desactiva la relación de aspecto.
@export var aspect_ratio := 0.0:
	set(value):
		aspect_ratio = value
		_request_apply()


@export_subgroup("Box Model")

# Vector4(left, top, right, bottom)
@export var css_margin := Vector4.ZERO:
	set(value):
		css_margin = value
		_request_apply()

@export var css_padding := Vector4.ZERO:
	set(value):
		css_padding = value
		_request_apply()

@export var box_sizing: BoxSizing = BoxSizing.BORDER_BOX:
	set(value):
		box_sizing = value
		_request_apply()


# ============================================================
# INTERNAL STATE
# ============================================================

var applying_layout := false

signal layout_applied


# ============================================================
# LIFECYCLE
# ============================================================

func _ready() -> void:
	var viewport := get_viewport()

	if not viewport.size_changed.is_connected(_on_viewport_size_changed):
		viewport.size_changed.connect(_on_viewport_size_changed)

	_connect_parent()

	_apply_css()


func _connect_parent() -> void:
	var p := get_parent()

	if p is Control:
		if not p.resized.is_connected(_on_parent_resized):
			p.resized.connect(_on_parent_resized)


func _on_viewport_size_changed() -> void:
	_apply_css()


func _on_parent_resized() -> void:
	_apply_css()


# ============================================================
# UPDATE
# ============================================================

func _request_apply() -> void:
	if not is_inside_tree():
		return

	if applying_layout:
		return

	if not is_queued_for_deletion():
		call_deferred("_apply_css")


# ============================================================
# REFERENCE RECT
# ============================================================

func _get_viewport_reference() -> Vector2:
	if Engine.is_editor_hint():
		return Vector2(
			ProjectSettings.get_setting(
				"display/window/size/viewport_width"
			),
			ProjectSettings.get_setting(
				"display/window/size/viewport_height"
			)
		)

	return get_viewport_rect().size


func get_css_content_rect() -> Rect2:
	return Rect2(
		Vector2(css_padding.x, css_padding.y),
		Vector2(
			maxf(0.0, size.x - css_padding.x - css_padding.z),
			maxf(0.0, size.y - css_padding.y - css_padding.w)
		)
	)


func _get_reference_rect() -> Rect2:
	var p := get_parent()

	if p is Control:
		if p is ControlCSS:
			return p.get_css_content_rect()

		return Rect2(Vector2.ZERO, p.size)

	return Rect2(Vector2.ZERO, _get_viewport_reference())


# ============================================================
# UNIT CONVERSION
# ============================================================

func _resolve_limit_axis(
	value: float,
	unit: CSSUnit,
	parent_size: float,
	viewport: Vector2,
	is_max: bool
) -> float:

	if unit == CSSUnit.AUTO:
		return -1.0 if is_max else 0.0

	if is_max and value < 0.0:
		return -1.0

	match unit:
		CSSUnit.PX:
			return value

		CSSUnit.PARENT_PERCENT:
			return parent_size * value / 100.0

		CSSUnit.VW:
			return viewport.x * value / 100.0

		CSSUnit.VH:
			return viewport.y * value / 100.0

	return value


func _resolve_limit_size(is_max: bool) -> Vector2:
	var reference := _get_reference_rect()
	var viewport := _get_viewport_reference()

	var values := css_max_size if is_max else css_min_size

	var width_unit_limit := (
		max_width_unit if is_max else min_width_unit
	)

	var height_unit_limit := (
		max_height_unit if is_max else min_height_unit
	)

	return Vector2(
		_resolve_limit_axis(
			values.x,
			width_unit_limit,
			reference.size.x,
			viewport,
			is_max
		),
		_resolve_limit_axis(
			values.y,
			height_unit_limit,
			reference.size.y,
			viewport,
			is_max
		)
	)

func _resolve_unit(
	value: float,
	unit: int,
	parent_size: float,
	viewport_size: float
) -> float:

	match unit:
		CSSUnit.PX:
			return value

		CSSUnit.PARENT_PERCENT:
			return parent_size * value / 100.0

		CSSUnit.VW:
			return viewport_size * value / 100.0

		CSSUnit.VH:
			return viewport_size * value / 100.0

	return value


func _resolve_position() -> Vector2:
	var reference := _get_reference_rect()
	var viewport := _get_viewport_reference()

	match position_unit:
		PositionUnit.PX:
			return css_position

		PositionUnit.PARENT_PERCENT:
			return Vector2(
				reference.size.x * css_position.x / 100.0,
				reference.size.y * css_position.y / 100.0
			)

		PositionUnit.VW:
			return Vector2(
				viewport.x * css_position.x / 100.0,
				viewport.y * css_position.y / 100.0
			)

		PositionUnit.VH:
			return Vector2(
				viewport.x * css_position.x / 100.0,
				viewport.y * css_position.y / 100.0
			)

	return css_position


# ============================================================
# SIZE CALCULATION
# ============================================================

func _resolve_size() -> Vector2:
	var reference := _get_reference_rect()
	var viewport := _get_viewport_reference()

	var available_width := maxf(
		0.0,
		reference.size.x - css_margin.x - css_margin.z
	)

	var available_height := maxf(
		0.0,
		reference.size.y - css_margin.y - css_margin.w
	)

	var intrinsic := get_combined_minimum_size()

	var width := _resolve_unit(
		css_size.x,
		width_unit,
		reference.size.x,
		viewport.x
	)

	var height := _resolve_unit(
		css_size.y,
		height_unit,
		reference.size.y,
		viewport.y
	)

	# AUTO
	if width_unit == CSSUnit.AUTO:
		width = available_width

	if height_unit == CSSUnit.AUTO:
		height = intrinsic.y

	# CONTENT BOX
	if box_sizing == BoxSizing.CONTENT_BOX:
		if width_unit != CSSUnit.AUTO:
			width += css_padding.x + css_padding.z
		else:
			width = maxf(
				0.0,
				width - css_padding.x - css_padding.z
			)
			width += css_padding.x + css_padding.z

		if height_unit != CSSUnit.AUTO:
			height += css_padding.y + css_padding.w
		else:
			height += css_padding.y + css_padding.w

	var result := Vector2(width, height)

	# MIN SIZE
	var min_size := custom_minimum_size

	result.x = maxf(result.x, min_size.x)
	result.y = maxf(result.y, min_size.y)

	# MAX SIZE
	var max_size := _resolve_limit_size(true)

	if max_size.x >= 0.0:
		result.x = minf(result.x, max_size.x)

	if max_size.y >= 0.0:
		result.y = minf(result.y, max_size.y)

	# ASPECT RATIO
	if aspect_ratio > 0.0:
		result.y = result.x / aspect_ratio

	return result


# ============================================================
# ANCHOR POSITIONING
# ============================================================

func _get_screen_position() -> Vector2:
	var reference := _get_reference_rect()

	var control_size := size
	var offset := _resolve_position()

	var left := css_margin.x
	var top := css_margin.y
	var right := css_margin.z
	var bottom := css_margin.w

	var center_x := (
		reference.size.x - control_size.x
	) / 2.0

	var center_y := (
		reference.size.y - control_size.y
	) / 2.0

	match screen_anchor:

		ScreenAnchor.TOP_LEFT:
			return reference.position + offset + Vector2(left, top)

		ScreenAnchor.TOP_CENTER:
			return reference.position + Vector2(
				center_x + offset.x + (left - right) / 2.0,
				offset.y + top
			)

		ScreenAnchor.TOP_RIGHT:
			return reference.position + Vector2(
				reference.size.x - control_size.x - offset.x - right,
				offset.y + top
			)

		ScreenAnchor.CENTER_LEFT:
			return reference.position + Vector2(
				offset.x + left,
				center_y + offset.y + (top - bottom) / 2.0
			)

		ScreenAnchor.CENTER:
			return reference.position + Vector2(
				center_x + offset.x + (left - right) / 2.0,
				center_y + offset.y + (top - bottom) / 2.0
			)

		ScreenAnchor.CENTER_RIGHT:
			return reference.position + Vector2(
				reference.size.x - control_size.x - offset.x - right,
				center_y + offset.y + (top - bottom) / 2.0
			)

		ScreenAnchor.BOTTOM_LEFT:
			return reference.position + Vector2(
				offset.x + left,
				reference.size.y - control_size.y - offset.y - bottom
			)

		ScreenAnchor.BOTTOM_CENTER:
			return reference.position + Vector2(
				center_x + offset.x + (left - right) / 2.0,
				reference.size.y - control_size.y - offset.y - bottom
			)

		ScreenAnchor.BOTTOM_RIGHT:
			return reference.position + Vector2(
				reference.size.x - control_size.x - offset.x - right,
				reference.size.y - control_size.y - offset.y - bottom
			)

	return reference.position + offset


# ============================================================
# APPLY LAYOUT
# ============================================================

func _apply_css() -> void:
	if applying_layout:
		return

	if not is_inside_tree():
		return

	applying_layout = true

	# Godot will use absolute local coordinates.
	anchor_left = 0.0
	anchor_top = 0.0
	anchor_right = 0.0
	anchor_bottom = 0.0

	# Aplicar CSS MIN SIZE
	custom_minimum_size = _resolve_limit_size(false)

	# Calculate dimensions before position.
	size = _resolve_size()

	if not get_parent() is Container:
		position = _get_screen_position()

	applying_layout = false

	layout_applied.emit()
