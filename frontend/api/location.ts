const TENCENT_MAP_KEY = 'EOEBZ-PRZEN-3Z7FR-SPYGR-2PYM7-L6F5K'

export interface ReverseGeocoderResult {
  address: string
  recommendAddress: string
  province: string
  city: string
  district: string
  street: string
  streetNumber: string
}

export function reverseGeocoder(latitude: number, longitude: number): Promise<ReverseGeocoderResult> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: 'https://apis.map.qq.com/ws/geocoder/v1/',
      method: 'GET',
      data: {
        location: `${latitude},${longitude}`,
        key: TENCENT_MAP_KEY,
        get_poi: 1,
        output: 'json'
      },
      success: (res: any) => {
        const data = res.data

        console.log('腾讯逆地址解析结果：', data)

        if (data.status !== 0) {
          reject(data)
          return
        }

        const result = data.result
        const component = result.address_component || {}

        resolve({
          address: result.address || '',
          recommendAddress: result.formatted_addresses?.recommend || result.address || '',
          province: component.province || '',
          city: component.city || '',
          district: component.district || '',
          street: component.street || '',
          streetNumber: component.street_number || ''
        })
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}