<script>
import { useAuth } from '@/composables/useAuth'

export default {
	onLaunch: function() {
		const { loadFromStorage } = useAuth()
		loadFromStorage()
	},
	onShow: function() {},
	onHide: function() {}
}
</script>

<style>
	/*每个页面公共css */
</style>
